package common.util;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class TokenizerUtil {

    private static final Set<String> STOPWORDS = Set.of(
            "de", "la", "el", "los", "las", "un", "una",
            "y", "en", "para", "con", "por", "que"
    );

    public static Set<String> tokenize(String text) {
        return Arrays.stream(normalize(text).split("\\s+"))
                .filter(t -> !t.isBlank())
                .filter(t -> !STOPWORDS.contains(t))
                .map(TokenizerUtil::stem)
                .collect(Collectors.toSet());
    }

    private static String normalize(String input) {
        String sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

    private static String stem(String token) {
        if (token.endsWith("os")) return token.substring(0, token.length() - 2);
        if (token.endsWith("as")) return token.substring(0, token.length() - 2);
        if (token.endsWith("es")) return token.substring(0, token.length() - 2);
        if (token.endsWith("o"))  return token.substring(0, token.length() - 1);
        if (token.endsWith("a"))  return token.substring(0, token.length() - 1);
        return token;
    }

    private TokenizerUtil() {}
}