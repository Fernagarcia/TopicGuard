package forumbot.service;

import common.service.MetricsService;
import forumbot.service.similarity.SimilarityResult;
import forumbot.service.similarity.SimilarityService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class ThreadService {

    private final SimilarityService similarityService;
    private final DecisionService decisionService;
    private final MetricsService metricsService;

    public ThreadService(SimilarityService similarityService,
                         DecisionService decisionService,
                         MetricsService metricsService) {
        this.similarityService = similarityService;
        this.decisionService = decisionService;
        this.metricsService = metricsService;
    }

    public void handleValidMessage(MessageReceivedEvent event,
                                   TextChannel channel,
                                   String slug,
                                   String contenido) {

        metricsService.incrementMessagesProcessed();

        String nombreThread = normalizar(slug);

        List<SimilarityResult> matches = similarityService.findMatches(
                nombreThread,
                channel.getThreadChannels()
        );

        if (!matches.isEmpty()) {
            SimilarityResult best = matches.get(0);

            switch (best.type()) {

                case EXACT -> {
                    redirigirDirecto(event, best.thread(), contenido);
                    metricsService.incrementRedirectedExact();
                    return;
                }

                case SIMILAR -> {
                    decisionService.solicitarConfirmacion(
                            event,
                            channel,
                            nombreThread,
                            contenido,
                            best.thread()
                    );
                    metricsService.incrementConfirmationRequested();
                    return;
                }

                case NONE -> {}
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

    private String normalizar(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-");
    }
}