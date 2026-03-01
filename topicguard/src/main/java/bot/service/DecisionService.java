package bot.service;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.Map;
import java.util.concurrent.*;

public class DecisionService {
    private final MetricsService metricsService;

    private final Map<Long, PendingDecision> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private static final String ACCEPT = "✅";
    private static final String REJECT = "❌";

    public DecisionService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public void solicitarConfirmacion(
            MessageReceivedEvent event,
            TextChannel channel,
            String nombreThread,
            String contenido,
            ThreadChannel similarThread) {

        channel.sendMessage(
                event.getAuthor().getAsMention() +
                        " Existe un hilo similar: " +
                        similarThread.getAsMention() +
                        "\nReacciona con ✅ para usarlo o ❌ para crear uno"
        ).queue(msg -> {

            msg.addReaction(Emoji.fromUnicode(ACCEPT)).queue();
            msg.addReaction(Emoji.fromUnicode(REJECT)).queue();

            ScheduledFuture<?> timeout =
                    scheduler.schedule(() ->
                                    channel.retrieveMessageById(msg.getId())
                                            .queue(m -> m.delete().queue()),
                            60,
                            TimeUnit.SECONDS
                    );

            pending.put(event.getAuthor().getIdLong(),
                    new PendingDecision(
                            channel,
                            nombreThread,
                            contenido,
                            similarThread,
                            msg.getIdLong(),
                            timeout
                    )
            );
        });

        event.getMessage().delete().queue();
    }

    public void handleReaction(MessageReactionAddEvent event) {

        if (event.getUser() == null || event.getUser().isBot()) return;

        PendingDecision decision =
                pending.get(event.getUserIdLong());

        if (decision == null) return;
        if (event.getMessageIdLong() != decision.messageId()) return;

        decision.timeout().cancel(true);

        String emoji = event.getReaction().getEmoji().getName();

        if (ACCEPT.equals(emoji)) {
            decision.similarThread()
                    .sendMessage(event.getUser().getAsMention() +
                            "\n" + decision.contenido())
                    .queue();

            metricsService.incrementConfirmationAccepted();
        }

        if (REJECT.equals(emoji)) {
            decision.channel()
                    .createThreadChannel(decision.nombreThread())
                    .queue(thread ->
                            thread.sendMessage(
                                    event.getUser().getAsMention() +
                                            "\n" + decision.contenido()
                            ).queue());

            metricsService.incrementConfirmationRejected();
        }

        decision.channel()
                .retrieveMessageById(decision.messageId())
                .queue(m -> m.delete().queue());

        pending.remove(event.getUserIdLong());
    }

    private record PendingDecision(
            TextChannel channel,
            String nombreThread,
            String contenido,
            ThreadChannel similarThread,
            long messageId,
            ScheduledFuture<?> timeout
    ) {}
}