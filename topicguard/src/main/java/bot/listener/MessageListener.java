package bot.listener;

import bot.service.TemplateService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class MessageListener extends ListenerAdapter {

    private final TemplateService templateService = new TemplateService();

    private final Map<Long, PendingDecision> pendingDecisions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private static final long DECISION_TIMEOUT_SECONDS = 60;

    private static final String EMOJI_ACCEPT = "✅";
    private static final String EMOJI_REJECT = "❌";

    private record PendingDecision(
            TextChannel channel,
            String nombreThread,
            String contenido,
            ThreadChannel similarThread,
            long confirmationMessageId,
            ScheduledFuture<?> timeoutTask
    ) {}

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;
        if (!(event.getChannel() instanceof TextChannel channel)) return;

        String mensaje = event.getMessage().getContentRaw();
        var resultado = templateService.validarMensaje(channel, mensaje);

        if (resultado == null) return;

        if (!resultado.valido()) {
            event.getMessage().delete().queue();
            channel.sendMessage(
                    event.getAuthor().getAsMention() +
                            " Formato incorrecto.\nUsa el comando configurado en el canal."
            ).queue();
            return;
        }

        String slug = resultado.valores().get("slug");
        String nombreThread = normalizarNombre(slug);

        String contenidoLimpio = mensaje.replaceFirst(
                "^!\\w+\\s+" + Pattern.quote(slug),
                ""
        ).trim();

        // 1️⃣ Buscar hilo similar activo
        Optional<ThreadChannel> threadSimilar = channel.getThreadChannels()
                .stream()
                .filter(thread -> esSimilar(thread.getName(), nombreThread))
                .findFirst();

        if (threadSimilar.isPresent()) {

            ThreadChannel similar = threadSimilar.get();

            channel.sendMessage(
                    event.getAuthor().getAsMention() +
                            " Existe un hilo similar: " +
                            similar.getAsMention() +
                            "\nReacciona con ✅ para usarlo o ❌ para crear uno nuevo."
            ).queue((Message msg) -> {

                msg.addReaction(Emoji.fromUnicode(EMOJI_ACCEPT)).queue();
                msg.addReaction(Emoji.fromUnicode(EMOJI_REJECT)).queue();

                ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {

                    PendingDecision pending =
                            pendingDecisions.remove(event.getAuthor().getIdLong());

                    if (pending != null) {
                        channel.retrieveMessageById(pending.confirmationMessageId())
                                .queue(m -> m.delete().queue(), failure -> {});
                    }

                }, DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                pendingDecisions.put(
                        event.getAuthor().getIdLong(),
                        new PendingDecision(
                                channel,
                                nombreThread,
                                contenidoLimpio,
                                similar,
                                msg.getIdLong(),
                                timeoutTask
                        )
                );
            });

            event.getMessage().delete().queue();
            return;
        }

        // 2️⃣ Buscar archivado
        channel.retrieveArchivedPublicThreadChannels().queue(archivedThreads -> {

            Optional<ThreadChannel> threadArchivado = archivedThreads.stream()
                    .filter(thread -> esSimilar(thread.getName(), nombreThread))
                    .findFirst();

            if (threadArchivado.isPresent()) {

                ThreadChannel thread = threadArchivado.get();
                thread.getManager().setArchived(false).queue();

                thread.sendMessage(
                        event.getAuthor().getAsMention() +
                                "\n" + contenidoLimpio
                ).queue();

                event.getMessage().delete().queue();

            } else {

                event.getMessage()
                        .createThreadChannel(nombreThread)
                        .queue(thread -> {

                            thread.sendMessage(
                                    event.getAuthor().getAsMention() +
                                            "\n" + contenidoLimpio
                            ).queue();

                            event.getMessage().delete().queue();
                        });
            }
        });
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        if (event.getUser() == null || event.getUser().isBot()) return;

        long userId = event.getUserIdLong();
        PendingDecision decision = pendingDecisions.get(userId);

        if (decision == null) return;
        if (event.getMessageIdLong() != decision.confirmationMessageId()) return;

        String emoji = event.getReaction().getEmoji().getName();

        decision.timeoutTask().cancel(true);

        if (EMOJI_ACCEPT.equals(emoji)) {

            decision.similarThread().sendMessage(
                    event.getUser().getAsMention() +
                            "\n" + decision.contenido()
            ).queue();
        }

        else if (EMOJI_REJECT.equals(emoji)) {

            decision.channel()
                    .createThreadChannel(decision.nombreThread())
                    .queue(thread -> {

                        thread.sendMessage(
                                event.getUser().getAsMention() +
                                        "\n" + decision.contenido()
                        ).queue();
                    });
        }

        decision.channel()
                .retrieveMessageById(decision.confirmationMessageId())
                .queue(m -> m.delete().queue(), failure -> {});

        pendingDecisions.remove(userId);
    }

    private String normalizarNombre(String input) {

        String sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private boolean esSimilar(String a, String b) {

        int distancia = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        double similitud = 1.0 - (double) distancia / maxLen;

        return similitud >= 0.85;
    }

    private int levenshtein(String a, String b) {

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            dp[i][0] = i;

        for (int j = 0; j <= b.length(); j++)
            dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {

                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}