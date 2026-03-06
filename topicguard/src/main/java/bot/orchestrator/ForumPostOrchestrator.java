package bot.orchestrator;

import bot.service.*;
import bot.service.similarity.SimilarityResult;
import bot.service.similarity.SimilarityService;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;

public class ForumPostOrchestrator {

    private final SimilarityService similarityService;
    private final ForumDuplicateService duplicateService;
    private final MetricsService metricsService;
    private final SpamService spamService;
    private final FeedbackService feedbackService;
    private final ThreadIndexService threadIndexService;

    public ForumPostOrchestrator(SimilarityService similarityService,
                                 ForumDuplicateService duplicateService,
                                 MetricsService metricsService,
                                 SpamService spamService,
                                 FeedbackService feedbackService,
                                 ThreadIndexService threadIndexService) {
        this.similarityService = similarityService;
        this.duplicateService = duplicateService;
        this.metricsService = metricsService;
        this.spamService = spamService;
        this.feedbackService = feedbackService;
        this.threadIndexService = threadIndexService;
    }

    public void processNewPost(ThreadChannel thread) {

        if (!(thread.getParentChannel() instanceof ForumChannel forum)) return;

        /*
        if (!spamService.permitidoEnForo(thread)) {
            feedbackService.mostrarCooldownEnForo(thread);
            return;
        }
        */

        // Usamos el índice en lugar de todos los threads del foro
        List<ThreadChannel> candidatos = threadIndexService.findCandidates(thread.getName());

        // Excluimos el hilo nuevo
        candidatos = candidatos.stream()
                .filter(t -> !t.getId().equals(thread.getId()))
                .toList();

        List<SimilarityResult> matches = similarityService.findMatches(
                thread.getName(),
                candidatos
        );

        if (!matches.isEmpty()) {
            duplicateService.handleDuplicates(thread, matches);
            metricsService.incrementConfirmationRequested();
        } else {
            threadIndexService.indexThread(thread);
            metricsService.incrementThreadsCreated();
        }
    }
}