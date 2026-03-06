package bot.service;

import bot.service.similarity.MatchType;
import bot.service.similarity.SimilarityResult;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;

public class ForumDuplicateService {

    private static final int MAX_SUGERENCIAS = 5;

    public void handleDuplicates(ThreadChannel nuevo, List<SimilarityResult> resultados) {

        List<SimilarityResult> exactos = resultados.stream()
                .filter(r -> r.type() == MatchType.EXACT)
                .toList();

        List<SimilarityResult> similares = resultados.stream()
                .filter(r -> r.type() == MatchType.SIMILAR)
                .toList();

        if (!exactos.isEmpty()) {
            sugerirExactos(nuevo, exactos);
        } else if (!similares.isEmpty()) {
            sugerirSimilares(nuevo, similares);
        }
    }

    private void sugerirExactos(ThreadChannel nuevo, List<SimilarityResult> exactos) {
        StringBuilder sb = new StringBuilder();
        sb.append("👋 Este tema ya existe:\n\n");

        exactos.stream()
                .limit(MAX_SUGERENCIAS)
                .forEach(r -> sb.append("• ").append(r.thread().getJumpUrl()).append("\n"));

        sb.append("\nTe recomendamos continuar la conversación ahí.");

        nuevo.sendMessage(sb.toString()).queue();
    }

    private void sugerirSimilares(ThreadChannel nuevo, List<SimilarityResult> similares) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤔 Encontramos temas similares al tuyo:\n\n");

        similares.stream()
                .limit(MAX_SUGERENCIAS)
                .forEach(r -> sb.append("• ").append(r.thread().getJumpUrl()).append("\n"));

        sb.append("\nSi tu duda es distinta, podés ignorar este mensaje.");

        nuevo.sendMessage(sb.toString()).queue();
    }
}