package bot.service.similarity;

import bot.engine.SimilarityEngine;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.text.Normalizer;
import java.util.*;

public class SimilarityService {

    private final SimilarityEngine titleEngine;
    private final double exactThreshold;
    private final double similarThreshold;

    public SimilarityService(SimilarityEngine titleEngine,
                             double exactThreshold,
                             double similarThreshold) {
        this.titleEngine = titleEngine;
        this.exactThreshold = exactThreshold;
        this.similarThreshold = similarThreshold;
    }

    public List<SimilarityResult> findMatches(
            String nuevoTitulo,
            List<ThreadChannel> candidatos) {

        if (candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }

        List<SimilarityResult> exactos = new ArrayList<>();
        List<SimilarityResult> similares = new ArrayList<>();

        for (ThreadChannel thread : candidatos) {

            double score = titleEngine.similarity(nuevoTitulo, thread.getName());
            MatchType type = classify(score);

            if (type == MatchType.NONE) continue;

            SimilarityResult result = new SimilarityResult(thread, score, type);

            if (type == MatchType.EXACT) {
                exactos.add(result);
            } else {
                similares.add(result);
            }
        }

        // Exactos primero, luego similares, ambos ordenados por score descendente
        List<SimilarityResult> all = new ArrayList<>();
        exactos.sort(Comparator.comparingDouble(SimilarityResult::score).reversed());
        similares.sort(Comparator.comparingDouble(SimilarityResult::score).reversed());
        all.addAll(exactos);
        all.addAll(similares);

        return all;
    }

    private MatchType classify(double score) {
        if (score >= exactThreshold) return MatchType.EXACT;
        if (score >= similarThreshold) return MatchType.SIMILAR;
        return MatchType.NONE;
    }

    private String normalize(String input) {

        String sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}