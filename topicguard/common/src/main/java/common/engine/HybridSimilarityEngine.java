package common.engine;

public class HybridSimilarityEngine implements SimilarityEngine {

    private final SimilarityEngine levenshtein;
    private final SimilarityEngine jaccard;
    private final SimilarityEngine containment;

    public HybridSimilarityEngine(SimilarityEngine levenshtein,
                                  SimilarityEngine jaccard,
                                  SimilarityEngine containment) {

        this.levenshtein = levenshtein;
        this.jaccard = jaccard;
        this.containment = containment;
    }

    @Override
    public double similarity(String a, String b) {

        double lev = levenshtein.similarity(a, b);
        double jac = jaccard.similarity(a, b);
        double con = containment.similarity(a, b);

        // Jaccard y Containment capturan similitud sin importar el orden
        // Levenshtein aporta precisión para títulos casi idénticos
        return (lev * 0.2) +
                (jac * 0.5) +
                (con * 0.3);
    }
}