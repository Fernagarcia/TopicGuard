package forumbot.orchestrator;

import common.service.MetricsService;
import forumbot.service.*;
import forumbot.service.similarity.MatchType;
import forumbot.service.similarity.SimilarityResult;
import forumbot.service.similarity.SimilarityService;
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
    private final LogService logService;

    public ForumPostOrchestrator(SimilarityService similarityService,
                                 ForumDuplicateService duplicateService,
                                 MetricsService metricsService,
                                 SpamService spamService,
                                 FeedbackService feedbackService,
                                 ThreadIndexService threadIndexService, LogService logService) {
        this.similarityService = similarityService;
        this.duplicateService = duplicateService;
        this.metricsService = metricsService;
        this.spamService = spamService;
        this.feedbackService = feedbackService;
        this.threadIndexService = threadIndexService;
        this.logService = logService;
    }

    public void processNewPost(ThreadChannel thread) {

        if (!(thread.getParentChannel() instanceof ForumChannel forum)) return;

        if (!spamService.permitidoEnForo(thread)) {
            feedbackService.mostrarCooldownEnForo(thread);
            return;
        }

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

            boolean soloExactos = matches.stream()
                    .allMatch(r -> r.type() == MatchType.EXACT);

            if (soloExactos) {
                logService.logForumCreated(thread); // solo el log de creación
                metricsService.incrementConfirmationRequested();
            } else {
                logService.logForumCreated(thread);
                metricsService.incrementThreadsCreated();
            }

        } else {
            threadIndexService.indexThread(thread);
            logService.logForumCreated(thread);
            metricsService.incrementThreadsCreated();
        }
    }
}