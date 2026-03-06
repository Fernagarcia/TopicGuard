package common.engine;

import java.text.Normalizer;
import java.util.Locale;

public class LevenshteinEngine implements SimilarityEngine {

    @Override
    public double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;

        a = normalize(a);
        b = normalize(b);

        if (a.equals(b)) return 1.0;

        if (onlyNumericTokenDiffers(a, b)) {
            return 0.0;
        }

        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;

        int distance = levenshtein(a, b);

        return 1.0 - ((double) distance / maxLen);
    }

    private String normalize(String input) {
        String sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "");
    }

    private boolean onlyNumericTokenDiffers(String a, String b) {
        String[] tokensA = a.split("-");
        String[] tokensB = b.split("-");

        if (tokensA.length != tokensB.length) return false;

        int numericDifferences = 0;

        for (int i = 0; i < tokensA.length; i++) {

            if (!tokensA[i].equals(tokensB[i])) {

                if (isNumeric(tokensA[i]) && isNumeric(tokensB[i])) {
                    numericDifferences++;
                } else {
                    return false;
                }
            }
        }

        return numericDifferences == 1;
    }

    private boolean isNumeric(String s) {
        return s.matches("\\d+");
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {

                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}