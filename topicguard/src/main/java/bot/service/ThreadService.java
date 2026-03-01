package bot.service;

import bot.service.similarity.SimilarityResult;
import bot.service.similarity.SimilarityService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadService {

    private final TemplateService templateService;
    private final SimilarityService similarityService;
    private final DecisionService decisionService;
    private final MetricsService metricsService;

    // 🔒 Anti-spam
    private final Map<Long, Long> lastCreation = new ConcurrentHashMap<>();
    private final Map<Long, String> lastSlug = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 360_000; // 5 minutos

    public ThreadService(TemplateService templateService,
                         SimilarityService similarityService,
                         DecisionService decisionService,
                         MetricsService metricsService) {
        this.templateService = templateService;
        this.similarityService = similarityService;
        this.decisionService = decisionService;
        this.metricsService = metricsService;
    }

    public void handleMessage(MessageReceivedEvent event) {

        metricsService.incrementMessagesProcessed();

        if (event.getAuthor().isBot()) return;
        if (!(event.getChannel() instanceof TextChannel channel)) return;

        String mensaje = event.getMessage().getContentRaw();
        var resultado = templateService.validarMensaje(channel, mensaje);

        if (resultado == null) return;

        if (!resultado.valido()) {
            avisarYBorrar(event, "Formato incorrecto.");
            return;
        }

        String slug = resultado.valores().get("slug");
        String nombreThread = normalizar(slug);
        String contenido = limpiarContenido(mensaje, slug);

        long userId = event.getAuthor().getIdLong();

        // Protección contra repetición inmediata
        String ultimoSlug = lastSlug.get(userId);
        if (nombreThread.equals(ultimoSlug)) {
            avisarYBorrar(event, "Ya enviaste ese mismo hilo recientemente.");
            return;
        }

        // Cooldown por usuario
        long now = System.currentTimeMillis();
        Long lastTime = lastCreation.get(userId);

        if (lastTime != null && now - lastTime < COOLDOWN_MS) {
            avisarYBorrar(event, "Tienes que esperar 5 minutos antes de crear otro hilo.");
            return;
        }

        Optional<SimilarityResult> matchResult =
                similarityService.findBestMatch(
                        nombreThread,
                        channel.getThreadChannels()
                );

        if (matchResult.isPresent()) {
            SimilarityResult result = matchResult.get();

            switch (result.type()) {

                case EXACT -> {
                    redirigirDirecto(event, result.thread(), contenido);
                    metricsService.incrementRedirectedExact();
                    lastSlug.put(userId, nombreThread);
                    return;
                }

                case SIMILAR -> {
                    decisionService.solicitarConfirmacion(
                            event,
                            channel,
                            nombreThread,
                            contenido,
                            result.thread()
                    );
                    metricsService.incrementConfirmationRequested();
                    return;
                }

                case NONE -> {
                }
            }
        }

        crearNuevoThread(event, nombreThread, contenido);
        metricsService.incrementThreadsCreated();

        // Guardamos control anti-spam
        lastCreation.put(userId, now);
        lastSlug.put(userId, nombreThread);
    }

    private void redirigirDirecto(MessageReceivedEvent event,
                                  ThreadChannel thread,
                                  String contenido) {

        thread.sendMessage(
                event.getAuthor().getAsMention() +
                        "\n" + contenido
        ).queue();

        event.getMessage().delete().queue();
    }

    private void crearNuevoThread(MessageReceivedEvent event,
                                  String nombre,
                                  String contenido) {

        event.getMessage()
                .createThreadChannel(nombre)
                .queue(thread -> {
                    thread.sendMessage(
                            event.getAuthor().getAsMention() +
                                    "\n" + contenido
                    ).queue();
                    event.getMessage().delete().queue();
                });
    }

    private void avisarYBorrar(MessageReceivedEvent event, String mensaje) {

        event.getChannel()
                .sendMessage(event.getAuthor().getAsMention() + " " + mensaje)
                .queue(msg ->
                        msg.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                );

        event.getMessage().delete().queue();
    }

    private boolean slugValido(String slug) {

        if (slug.length() < 4) return false;
        if (slug.matches("\\d+")) return false;
        if (slug.matches("(.)\\1{3,}")) return false;

        return true;
    }

    private String limpiarContenido(String mensaje, String slug) {
        return mensaje.replaceFirst("^!\\w+\\s+" + slug, "").trim();
    }

    private String normalizar(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-");
    }

    // ---------------- STATS ----------------

    public void handleStatsCommand(SlashCommandInteractionEvent event) {

        if (!esAdmin(event)) {
            event.reply("No tenés permisos para usar este comando.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String mensaje = """
            📊 **Estadísticas del Bot**

            Mensajes procesados: %d
            Threads creados: %d
            Redirecciones exactas: %d
            Confirmaciones solicitadas: %d
            Confirmaciones aceptadas: %d
            Confirmaciones rechazadas: %d
            Tasa aceptación: %.2f%%
            """.formatted(
                metricsService.getMessagesProcessed(),
                metricsService.getThreadsCreated(),
                metricsService.getRedirectedExact(),
                metricsService.getConfirmationRequested(),
                metricsService.getConfirmationAccepted(),
                metricsService.getConfirmationRejected(),
                metricsService.getConfirmationAcceptRate() * 100
        );

        event.reply(mensaje)
                .setEphemeral(true)
                .queue();
    }

    private boolean esAdmin(SlashCommandInteractionEvent event) {
        return event.getMember() != null &&
                event.getMember().hasPermission(Permission.ADMINISTRATOR);
    }
}