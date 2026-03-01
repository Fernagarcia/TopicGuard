package bot.service.similarity;

import bot.engine.SimilarityEngine;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;
import java.util.Optional;

public class SimilarityService {

    private final SimilarityEngine engine;
    private final double exactThreshold;
    private final double similarThreshold;

    public SimilarityService(SimilarityEngine engine,
                             double exactThreshold,
                             double similarThreshold) {
        this.engine = engine;
        this.exactThreshold = exactThreshold;
        this.similarThreshold = similarThreshold;
    }

    public Optional<SimilarityResult> findBestMatch(
            String nombre,
            List<ThreadChannel> threads) {

        if (threads == null || threads.isEmpty()) {
            return Optional.empty();
        }

        SimilarityResult best = null;

        for (ThreadChannel thread : threads) {
            String threadName = thread.getName();
            double score = engine.similarity(nombre, threadName);

            MatchType type = classify(score);

            if (type == MatchType.NONE) {
                continue;
            }

            // Si es EXACT podemos cortar inmediatamente
            if (type == MatchType.EXACT) {
                return Optional.of(
                        new SimilarityResult(thread, score, MatchType.EXACT)
                );
            }

            // Para SIMILAR buscamos el mejor score
            if (best == null || score > best.score()) {
                best = new SimilarityResult(thread, score, type);
            }
        }

        return Optional.ofNullable(best);
    }

    private MatchType classify(double score) {
        if (score >= exactThreshold) {
            return MatchType.EXACT;
        }

        if (score >= similarThreshold) {
            return MatchType.SIMILAR;
        }

        return MatchType.NONE;
    }
}