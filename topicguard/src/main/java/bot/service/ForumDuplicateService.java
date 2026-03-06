package bot.service;

import bot.service.similarity.MatchType;
import bot.service.similarity.SimilarityResult;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForumDuplicateService {

    private static final int MAX_SUGERENCIAS = 5;
    private final DecisionService decisionService;
    private final LogService logService;

    public ForumDuplicateService(DecisionService decisionService, LogService logService) {
        this.decisionService = decisionService;
        this.logService = logService;
    }

    public void handleDuplicates(ThreadChannel nuevo, List<SimilarityResult> resultados) {

        List<SimilarityResult> exactos = resultados.stream()
                .filter(r -> r.type() == MatchType.EXACT)
                .toList();

        List<SimilarityResult> similares = resultados.stream()
                .filter(r -> r.type() == MatchType.SIMILAR)
                .toList();

        if (!exactos.isEmpty()) {
            cerrarConRedireccion(nuevo, exactos);
        } else if (!similares.isEmpty()) {
            decisionService.solicitarConfirmacionEnForo(nuevo, similares);
        }
    }

    private void cerrarConRedireccion(ThreadChannel nuevo, List<SimilarityResult> exactos) {
        StringBuilder sb = new StringBuilder();
        sb.append("👋 Este tema ya existe:\n\n");

        exactos.stream()
                .limit(MAX_SUGERENCIAS)
                .forEach(r -> sb.append("• ").append(r.thread().getJumpUrl()).append("\n"));

        sb.append("\nContinuá la conversación ahí. Este foro se cerrará en 30 segundos.");

        nuevo.sendMessage(sb.toString())
                .queue(msg -> {
                    nuevo.getManager()
                            .setArchived(true)
                            .setLocked(true)
                            .queueAfter(30, TimeUnit.SECONDS);

                    logService.logForumClosedExact(nuevo); // log aquí
                });
    }
}