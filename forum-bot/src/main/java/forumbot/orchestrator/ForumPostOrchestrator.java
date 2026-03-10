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
    private final TagService tagService;
    private final ValidationService validationService; // nuevo

    public ForumPostOrchestrator(SimilarityService similarityService,
                                 ForumDuplicateService duplicateService,
                                 MetricsService metricsService,
                                 SpamService spamService,
                                 FeedbackService feedbackService,
                                 ThreadIndexService threadIndexService,
                                 LogService logService,
                                 TagService tagService,
                                 ValidationService validationService) {
        this.similarityService = similarityService;
        this.duplicateService = duplicateService;
        this.metricsService = metricsService;
        this.spamService = spamService;
        this.feedbackService = feedbackService;
        this.threadIndexService = threadIndexService;
        this.logService = logService;
        this.tagService = tagService;
        this.validationService = validationService;
    }

    public void processNewPost(ThreadChannel thread) {

        if (!(thread.getParentChannel() instanceof ForumChannel forum)) return;

        // 1. Validar título antes de cualquier otra cosa
        if (!validationService.tituloValido(thread.getName())) {
            validationService.rechazarTituloPorLongitud(thread);
            return;
        }

        // 2. Validar cooldown
        if (!spamService.permitidoEnForo(thread)) {
            feedbackService.mostrarCooldownEnForo(thread);
            return;
        }

        // 3. Buscar duplicados
        List<ThreadChannel> candidatos = threadIndexService.findCandidates(thread.getName())
                .stream()
                .filter(t -> !t.getId().equals(thread.getId()))
                .toList();

        List<SimilarityResult> matches = similarityService.findMatches(
                thread.getName(),
                candidatos
        );

        tagService.aplicarTagInicial(thread);

        if (!matches.isEmpty()) {
            duplicateService.handleDuplicates(thread, matches);

            boolean soloExactos = matches.stream()
                    .allMatch(r -> r.type() == MatchType.EXACT);

            logService.logForumCreated(thread);
            if (soloExactos) {
                logService.logForumClosedExact(thread);
                metricsService.incrementConfirmationRequested();
            } else {
                metricsService.incrementThreadsCreated();
            }

        } else {
            threadIndexService.indexThread(thread);
            logService.logForumCreated(thread);
            metricsService.incrementThreadsCreated();
        }
    }
}