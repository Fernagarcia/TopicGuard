package bot.service;

import bot.service.similarity.SimilarityResult;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DecisionService {

    private final MetricsService metricsService;
    private final LogService logService;
    private final Map<Long, PendingDecision> pending = new ConcurrentHashMap<>();
    private final Map<Long, PendingForumDecision> pendingForum = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    private static final String ACCEPT = "✅";
    private static final String REJECT = "❌";

    public DecisionService(MetricsService metricsService, LogService logService) {
        this.metricsService = metricsService;
        this.logService = logService;
    }

    // -------------------- Canal de texto (existente) --------------------

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
                            60, TimeUnit.SECONDS
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

        // Intentamos resolver como decisión de canal de texto
        PendingDecision decision = pending.get(event.getUserIdLong());
        if (decision != null && event.getMessageIdLong() == decision.messageId()) {
            handleTextDecision(event, decision);
            return;
        }

        // Intentamos resolver como decisión de foro
        PendingForumDecision forumDecision = pendingForum.get(event.getUserIdLong());
        if (forumDecision != null && event.getMessageIdLong() == forumDecision.messageId()) {
            handleForumDecision(event, forumDecision);
        }
    }

    // -------------------- Foro --------------------

    public void solicitarConfirmacionEnForo(
            ThreadChannel nuevoForo,
            List<SimilarityResult> similares) {

        StringBuilder sb = new StringBuilder();
        sb.append("🤔 Encontramos temas similares al tuyo:\n\n");

        similares.stream()
                .limit(3)
                .forEach(r -> sb.append("• ").append(r.thread().getJumpUrl()).append("\n"));

        sb.append("\n<@").append(nuevoForo.getOwnerIdLong()).append("> ");
        sb.append("¿Tu duda ya está respondida en alguno de esos foros?\n");
        sb.append("✅ Sí, cierren este foro | ❌ No, mi duda es distinta\n\n");
        sb.append("⏳ Tenés 30 minutos para decidir. Si no respondés, este foro se cerrará automáticamente.");

        nuevoForo.sendMessage(sb.toString()).queue(msg -> {

            msg.addReaction(Emoji.fromUnicode(ACCEPT)).queue();
            msg.addReaction(Emoji.fromUnicode(REJECT)).queue();

            ScheduledFuture<?> timeout = scheduler.schedule(() -> {
                        nuevoForo.sendMessage("⏰ No recibimos respuesta, cerrando este foro automáticamente.")
                                .queue(m -> nuevoForo.getManager()
                                        .setArchived(true)
                                        .setLocked(true)
                                        .queue());

                        logService.logForumClosedByTimeout(nuevoForo); // log cierre por timeout
                        pendingForum.remove(nuevoForo.getOwnerIdLong());
                    },
                    30, TimeUnit.MINUTES
            );

            pendingForum.put(nuevoForo.getOwnerIdLong(),
                    new PendingForumDecision(
                            nuevoForo,
                            msg.getIdLong(),
                            timeout
                    )
            );
        });
    }

    private void handleTextDecision(MessageReactionAddEvent event, PendingDecision decision) {
        decision.timeout().cancel(true);
        String emoji = event.getReaction().getEmoji().getName();

        if (ACCEPT.equals(emoji)) {
            decision.similarThread()
                    .sendMessage(event.getUser().getAsMention() + "\n" + decision.contenido())
                    .queue();
            metricsService.incrementConfirmationAccepted();
        }

        if (REJECT.equals(emoji)) {
            decision.channel()
                    .createThreadChannel(decision.nombreThread())
                    .queue(thread ->
                            thread.sendMessage(
                                    event.getUser().getAsMention() + "\n" + decision.contenido()
                            ).queue());
            metricsService.incrementConfirmationRejected();
        }

        decision.channel()
                .retrieveMessageById(decision.messageId())
                .queue(m -> m.delete().queue());

        pending.remove(event.getUserIdLong());
    }

    private void handleForumDecision(MessageReactionAddEvent event, PendingForumDecision decision) {
        if (event.getUserIdLong() != decision.nuevoForo().getOwnerIdLong()) return;

        decision.timeout().cancel(true);
        String emoji = event.getReaction().getEmoji().getName();

        if (ACCEPT.equals(emoji)) {
            decision.nuevoForo().sendMessage("👋 Entendido, cerramos este foro en 60 segundos.")
                    .queue(msg -> decision.nuevoForo().getManager()
                            .setArchived(true)
                            .setLocked(true)
                            .queueAfter(60, TimeUnit.SECONDS));

            logService.logForumClosedByUser(decision.nuevoForo()); // log cierre por usuario
            metricsService.incrementConfirmationAccepted();
        }

        if (REJECT.equals(emoji)) {
            decision.nuevoForo()
                    .sendMessage("✅ Entendido, este foro se mantiene abierto.")
                    .queue();

            metricsService.incrementConfirmationRejected();
        }

        pendingForum.remove(event.getUserIdLong());
    }

    // -------------------- Records --------------------

    private record PendingDecision(
            TextChannel channel,
            String nombreThread,
            String contenido,
            ThreadChannel similarThread,
            long messageId,
            ScheduledFuture<?> timeout
    ) {}

    private record PendingForumDecision(
            ThreadChannel nuevoForo,
            long messageId,
            ScheduledFuture<?> timeout
    ) {}
}