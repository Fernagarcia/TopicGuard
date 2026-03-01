package bot.service;

import bot.service.similarity.SimilarityResult;
import bot.service.similarity.SimilarityService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Optional;

public class ThreadService {

    private final TemplateService templateService;
    private final SimilarityService similarityService;
    private final DecisionService decisionService;
    private final MetricsService metricsService;

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
            event.getMessage().delete().queue();
            channel.sendMessage(
                    event.getAuthor().getAsMention() +
                            " Formato incorrecto."
            ).queue();
            return;
        }

        String slug = resultado.valores().get("slug");
        String nombreThread = normalizar(slug);
        String contenido = limpiarContenido(mensaje, slug);

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
                    // no hacemos nada, sigue flujo normal
                }
            }
        }

        crearNuevoThread(event, nombreThread, contenido);
        metricsService.incrementThreadsCreated();
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

    private String limpiarContenido(String mensaje, String slug) {
        return mensaje.replaceFirst("^!\\w+\\s+" + slug, "").trim();
    }

    private String normalizar(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-");
    }

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
                .setEphemeral(true) // solo lo ve el admin
                .queue();
    }

    private boolean esAdmin(SlashCommandInteractionEvent event) {
        return event.getMember() != null &&
                event.getMember().hasPermission(Permission.ADMINISTRATOR);
    }
}