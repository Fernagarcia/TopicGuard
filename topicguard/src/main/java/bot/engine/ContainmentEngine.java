package bot.engine;

import bot.engine.util.TokenizerUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ContainmentEngine implements SimilarityEngine {

    @Override
    public double similarity(String a, String b) {

        Set<String> tokensA = TokenizerUtil.tokenize(a);
        Set<String> tokensB = TokenizerUtil.tokenize(b);

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        int minSize = Math.min(tokensA.size(), tokensB.size());

        return (double) intersection.size() / minSize;
    }
}