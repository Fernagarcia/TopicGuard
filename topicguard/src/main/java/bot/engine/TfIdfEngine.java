package bot.engine;

import java.util.*;
import java.util.stream.Collectors;

public class TfIdfEngine implements SimilarityEngine {

    private static final Set<String> STOPWORDS = Set.of(
            "de", "la", "el", "los", "las", "un", "una",
            "y", "en", "para", "con", "por", "que"
    );

    @Override
    public double similarity(String a, String b) {
        List<String> corpus = List.of(a, b);

        Map<String, Double> v1 = tfidf(a, corpus);
        Map<String, Double> v2 = tfidf(b, corpus);

        return cosine(v1, v2);
    }

    private Map<String, Double> tfidf(String doc, List<String> corpus) {
        List<String> tokens = tokenize(doc);

        Map<String, Double> tf = new HashMap<>();

        for (String token : tokens)
            tf.merge(token, 1.0, Double::sum);

        int totalTerms = tokens.size();

        tf.replaceAll((k, v) -> v / totalTerms);

        Map<String, Double> tfidf = new HashMap<>();

        for (String term : tf.keySet()) {

            long docsWithTerm = corpus.stream()
                    .filter(d -> tokenize(d).contains(term))
                    .count();

            double idf = Math.log((double) corpus.size() / (1 + docsWithTerm));

            tfidf.put(term, tf.get(term) * idf);
        }

        return tfidf;
    }

    private double cosine(Map<String, Double> v1, Map<String, Double> v2) {
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(v1.keySet());
        allTerms.addAll(v2.keySet());

        double dot = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (String term : allTerms) {

            double a = v1.getOrDefault(term, 0.0);
            double b = v2.getOrDefault(term, 0.0);

            dot += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }

        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-10);
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(
                        text.toLowerCase()
                                .replaceAll("[^a-z0-9áéíóúñ ]", "")
                                .split("\\s+")
                )
                .filter(token -> !token.isBlank())
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toList());
    }
}