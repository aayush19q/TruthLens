package truthlens_backend.service;

import org.springframework.stereotype.Service;
import truthlens_backend.model.Evidence;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EvidenceAnalysisService {
    private final SemanticSimilarityService semanticSimilarityService;
    private final FactConsistencyService factConsistencyService;


    public EvidenceAnalysisService(
            SemanticSimilarityService semanticSimilarityService,
            FactConsistencyService factConsistencyService) {

        this.semanticSimilarityService =
                semanticSimilarityService;

        this.factConsistencyService =
                factConsistencyService;
    }
    // ============================================================
    // STOP WORDS
    // ============================================================

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "is", "was", "were", "a", "an", "and", "or", "of",
            "to", "in", "on", "for", "by", "with", "from", "as", "at",
            "this", "that", "it", "will", "has", "have", "had", "be",
            "been", "are", "their", "they", "them", "than", "into",
            "about", "after", "before", "during", "over", "under",
            "its", "his", "her", "which", "who", "what", "where",
            "when", "how", "can", "may", "also", "more", "most",
            "around", "approximately", "roughly"
    );

    // ============================================================
    // NUMBER PATTERNS
    // ============================================================

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "(?<![A-Za-z])(-?\\d+(?:\\.\\d+)?)\\s*" +
                            "(%|percent|percentage|crore|crores|lakh|lakhs|" +
                            "million|millions|billion|billions|trillion|trillions|" +
                            "cent|cents)?",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern YEAR_PATTERN =
            Pattern.compile(
                    "\\b(19\\d{2}|20\\d{2}|21\\d{2})\\b"
            );

    // ============================================================
    // CONCEPT GROUPS
    //
    // These allow TruthLens to understand common paraphrases.
    // ============================================================

    private static final Map<String, Set<String>> CONCEPT_GROUPS =
            Map.of(

                    "GDP_GROWTH",
                    Set.of(
                            "gdp",
                            "growth",
                            "grow",
                            "grew",
                            "growing",
                            "expand",
                            "expanded",
                            "expanding",
                            "expansion",
                            "increase",
                            "increased",
                            "increasing",
                            "rise",
                            "rose",
                            "rising",
                            "economic",
                            "economy"
                    ),

                    "PRICE_CHANGE",
                    Set.of(
                            "price",
                            "prices",
                            "cost",
                            "costs",
                            "rate",
                            "rates",
                            "expensive",
                            "cheaper",
                            "increase",
                            "increased",
                            "decrease",
                            "decreased",
                            "rise",
                            "rose",
                            "fall",
                            "fell",
                            "reduced",
                            "reduction",
                            "raised"
                    ),

                    "GOVERNMENT",
                    Set.of(
                            "government",
                            "govt",
                            "authority",
                            "authorities",
                            "official",
                            "officials",
                            "ministry",
                            "administration"
                    ),

                    "FUEL",
                    Set.of(
                            "fuel",
                            "petrol",
                            "diesel",
                            "gasoline",
                            "gas",
                            "energy"
                    ),

                    "EMPLOYMENT",
                    Set.of(
                            "employment",
                            "employed",
                            "job",
                            "jobs",
                            "unemployment",
                            "workers",
                            "workforce"
                    ),

                    "INFLATION",
                    Set.of(
                            "inflation",
                            "inflationary",
                            "consumer",
                            "prices",
                            "cpi",
                            "cost"
                    )
            );

    // ============================================================
    // MAIN ANALYSIS
    // ============================================================

    public List<Evidence> analyzeEvidence(
            String claim,
            List<Evidence> evidenceList) {

        if (evidenceList == null
                || evidenceList.isEmpty()) {

            return List.of();
        }

        for (Evidence evidence :
                evidenceList) {

            if (evidence == null) {
                continue;
            }

            String content =
                    evidence.getContent() == null
                            ? ""
                            : evidence.getContent();

            // ----------------------------------------------------
            // 1. Hybrid NLP similarity
            // ----------------------------------------------------

            double similarity =
                    calculateSimilarity(
                            claim,
                            content
                    );

            evidence.setSimilarityScore(
                    similarity
            );

            // ----------------------------------------------------
            // 2. Source reliability
            // ----------------------------------------------------

            double reliability =
                    calculateReliability(
                            evidence.getSourceName()
                    );

            evidence.setReliabilityScore(
                    reliability
            );

            // ----------------------------------------------------
            // 3. Numerical comparison
            // ----------------------------------------------------

            Comparison comparison =
                    compareNumbers(
                            claim,
                            content
                    );

            // ----------------------------------------------------
            // 4. Relationship
            // ----------------------------------------------------

            FactConsistencyService.ConsistencyResult consistency =
                    factConsistencyService.check(
                            claim,
                            evidence.getContent()
                    );

            String relationship =
                    determineRelationship(
                            similarity,
                            comparison,
                            consistency
                    );

            evidence.setRelationship(
                    relationship
            );


        double numericalStrength =
                calculateNumericalStrength(
                        claim,
                        evidence.getContent()
                );

        evidence.setNumericalStrength(
                numericalStrength
        );
    }

        // --------------------------------------------------------
        // Rank evidence
        // --------------------------------------------------------

        evidenceList.sort(
                Comparator
                        .comparingDouble(
                                this::calculateEvidenceScore
                        )
                        .reversed()
        );

        return evidenceList;
    }

    // ============================================================
    // RELATIONSHIP
    // ============================================================

    private String determineRelationship(
            double similarity,
            Comparison comparison,
            FactConsistencyService.ConsistencyResult consistency) {

        // ========================================================
        // 1. NUMERICAL CONTRADICTION
        // ========================================================
        //
        // Numerical contradiction gets the highest priority.
        //
        // Example:
        //
        // Claim:
        // India GDP growth was 75%.
        //
        // Evidence:
        // India GDP growth was 7.8%.
        //
        // If the number is relevant and the comparison says
        // the values contradict each other, classify it as
        // CONTRADICTS regardless of semantic similarity.
        // ========================================================

        if (comparison.contradicts()
                && comparison.relevantNumberFound()) {

            return "CONTRADICTS";
        }


        // ========================================================
        // 2. NUMERICAL SUPPORT
        // ========================================================
        //
        // If the evidence contains a relevant number that agrees
        // with the claim, classify it as SUPPORTS.
        //
        // Example:
        //
        // Claim:
        // GDP growth was 7.8%.
        //
        // Evidence:
        // GDP growth was approximately 7.8%.
        // ========================================================

        if (comparison.supports()
                && comparison.relevantNumberFound()) {

            return "SUPPORTS";
        }


        // ========================================================
        // 3. FACT CONSISTENCY CONTRADICTION
        // ========================================================

        if (consistency != null) {
            if ("TEMPORAL_AND_NUMERICAL_MISMATCH"
                    .equals(consistency.status())
                    || "NUMERICAL_MISMATCH"
                    .equals(consistency.status())) {

                return "CONTRADICTS";
            }
        }


        // ========================================================
        // 4. UNRELATED OR MISSING NUMBER
        // ========================================================
        //
        // Example:
        //
        // Claim:
        // GDP growth was 75%.
        //
        // Evidence:
        // India's population is 1.4 billion.
        //
        // The number is not related to the claim.
        // ========================================================

        if (comparison.unrelatedNumberFound()) {

            return "UNCLEAR";
        }

        if (comparison.comparableClaimNumberFound()
                && !comparison.relevantNumberFound()) {

            return "UNCLEAR";
        }


        // ========================================================
        // 5. STRONG SEMANTIC MATCH
        // ========================================================
        //
        // If there is no useful numerical comparison but the
        // Transformer finds strong semantic similarity, use the
        // semantic signal.
        // ========================================================

        if (similarity >= 0.50) {

            return "SUPPORTS";
        }


        // ========================================================
        // 6. OTHERWISE UNCLEAR
        // ========================================================

        return "UNCLEAR";
    }
    // ============================================================
    // HYBRID NLP SIMILARITY
    // ============================================================

    private double calculateLocalSimilarity(
            String claim,
            String evidence) {

        if (claim == null
                || evidence == null
                || claim.isBlank()
                || evidence.isBlank()) {

            return 0.0;
        }

        // --------------------------------------------------------
        // TF-IDF similarity
        // --------------------------------------------------------

        double tfidf =
                calculateTfIdfSimilarity(
                        claim,
                        evidence
                );

        // --------------------------------------------------------
        // Concept similarity
        // --------------------------------------------------------

        double conceptSimilarity =
                calculateConceptSimilarity(
                        claim,
                        evidence
                );

        // --------------------------------------------------------
        // Number similarity
        // --------------------------------------------------------

        double numberSimilarity =
                calculateNumberSimilarity(
                        claim,
                        evidence
                );

        /*
         * Hybrid score:
         *
         * TF-IDF       = 40%
         * Concepts     = 40%
         * Numbers      = 20%
         */

        double score =
                tfidf * 0.40
                        + conceptSimilarity * 0.40
                        + numberSimilarity * 0.20;

        return round(
                Math.min(
                        1.0,
                        score
                )
        );
    }

    private double calculateSimilarity(
            String claim,
            String evidence) {

        if (claim == null
                || evidence == null
                || claim.isBlank()
                || evidence.isBlank()) {

            return 0.0;
        }

        double transformerScore =
                semanticSimilarityService
                        .calculateSimilarity(
                                claim,
                                evidence
                        );

        if (transformerScore <= 0.0) {

            return calculateLocalSimilarity(
                    claim,
                    evidence
            );
        }

        return round(transformerScore);
    }

    // ============================================================
    // TF-IDF SIMILARITY
    // ============================================================

    private double calculateTfIdfSimilarity(
            String claim,
            String evidence) {

        List<String> claimTokens =
                tokenizeList(claim);

        List<String> evidenceTokens =
                tokenizeList(evidence);

        if (claimTokens.isEmpty()
                || evidenceTokens.isEmpty()) {

            return 0.0;
        }

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(
                claimTokens
        );

        vocabulary.addAll(
                evidenceTokens
        );

        Map<String, Double> claimVector =
                buildTfIdfVector(
                        claimTokens,
                        vocabulary,
                        evidenceTokens
                );

        Map<String, Double> evidenceVector =
                buildTfIdfVector(
                        evidenceTokens,
                        vocabulary,
                        claimTokens
                );

        return cosineSimilarity(
                claimVector,
                evidenceVector
        );
    }

    // ============================================================
    // CONCEPT SIMILARITY
    // ============================================================

    private double calculateConceptSimilarity(
            String claim,
            String evidence) {

        Set<String> claimConcepts =
                detectConcepts(claim);

        Set<String> evidenceConcepts =
                detectConcepts(evidence);

        if (claimConcepts.isEmpty()
                || evidenceConcepts.isEmpty()) {

            return 0.0;
        }

        Set<String> intersection =
                new HashSet<>(
                        claimConcepts
                );

        intersection.retainAll(
                evidenceConcepts
        );

        Set<String> union =
                new HashSet<>(
                        claimConcepts
                );

        union.addAll(
                evidenceConcepts
        );

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size()
                / union.size();
    }

    // ============================================================
    // DETECT CONCEPTS
    // ============================================================

    private Set<String> detectConcepts(
            String text) {

        Set<String> concepts =
                new HashSet<>();

        String normalized =
                normalize(text);

        Set<String> words =
                new HashSet<>(
                        Arrays.asList(
                                normalized.split("\\s+")
                        )
                );

        for (Map.Entry<String, Set<String>> entry :
                CONCEPT_GROUPS.entrySet()) {

            String conceptName =
                    entry.getKey();

            Set<String> conceptWords =
                    entry.getValue();

            for (String word :
                    words) {

                if (conceptWords.contains(word)) {

                    concepts.add(
                            conceptName
                    );

                    break;
                }
            }
        }

        return concepts;
    }

    // ============================================================
    // NUMBER SIMILARITY
    // ============================================================

    private double calculateNumberSimilarity(
            String claim,
            String evidence) {

        List<NumberFact> claimNumbers =
                extractNumbers(claim);

        List<NumberFact> evidenceNumbers =
                extractNumbers(evidence);

        if (claimNumbers.isEmpty()
                || evidenceNumbers.isEmpty()) {

            return 0.0;
        }

        double bestScore = 0.0;

        for (NumberFact claimNumber :
                claimNumbers) {

            if (claimNumber.year()) {
                continue;
            }

            for (NumberFact evidenceNumber :
                    evidenceNumbers) {

                if (evidenceNumber.year()) {
                    continue;
                }

                if (!claimNumber.unit()
                        .equals(
                                evidenceNumber.unit()
                        )) {

                    continue;
                }

                if (!sameNumberContext(
                        claimNumber,
                        evidenceNumber)) {

                    continue;
                }

                if (evidenceNumber.negated()) {

                    continue;
                }

                double difference =
                        Math.abs(
                                claimNumber.value()
                                        - evidenceNumber.value()
                        );

                double tolerance =
                        calculateTolerance(
                                claimNumber.unit(),
                                claimNumber.value()
                        );

                if (difference <= tolerance) {

                    bestScore =
                            Math.max(
                                    bestScore,
                                    1.0
                            );

                } else {

                    /*
                     * Smaller numerical differences
                     * receive partial similarity.
                     */

                    double relativeDifference =
                            difference
                                    / Math.max(
                                    Math.abs(
                                            claimNumber.value()
                                    ),
                                    1.0
                            );

                    double score =
                            Math.max(
                                    0.0,
                                    1.0
                                            - relativeDifference
                            );

                    bestScore =
                            Math.max(
                                    bestScore,
                                    score
                            );
                }
            }
        }

        return bestScore;
    }

    // ============================================================
    // TF-IDF VECTOR
    // ============================================================

    private Map<String, Double> buildTfIdfVector(
            List<String> document,
            Set<String> vocabulary,
            List<String> otherDocument) {

        Map<String, Double> vector =
                new HashMap<>();

        for (String word :
                vocabulary) {

            int termFrequency =
                    0;

            for (String token :
                    document) {

                if (token.equals(word)) {

                    termFrequency++;
                }
            }

            if (termFrequency == 0) {

                vector.put(
                        word,
                        0.0
                );

                continue;
            }

            double tf =
                    (double) termFrequency
                            / document.size();

            int documentsContainingWord =
                    1;

            if (otherDocument.contains(word)) {

                documentsContainingWord++;
            }

            double idf =
                    Math.log(
                            2.0
                                    / documentsContainingWord
                    ) + 1.0;

            vector.put(
                    word,
                    tf * idf
            );
        }

        return vector;
    }

    // ============================================================
    // COSINE
    // ============================================================

    private double cosineSimilarity(
            Map<String, Double> vectorA,
            Map<String, Double> vectorB) {

        Set<String> vocabulary =
                new HashSet<>();

        vocabulary.addAll(
                vectorA.keySet()
        );

        vocabulary.addAll(
                vectorB.keySet()
        );

        double dotProduct =
                0.0;

        double magnitudeA =
                0.0;

        double magnitudeB =
                0.0;

        for (String word :
                vocabulary) {

            double a =
                    vectorA.getOrDefault(
                            word,
                            0.0
                    );

            double b =
                    vectorB.getOrDefault(
                            word,
                            0.0
                    );

            dotProduct +=
                    a * b;

            magnitudeA +=
                    a * a;

            magnitudeB +=
                    b * b;
        }

        if (magnitudeA == 0
                || magnitudeB == 0) {

            return 0.0;
        }

        return dotProduct
                / (
                Math.sqrt(magnitudeA)
                        * Math.sqrt(magnitudeB)
        );
    }

    // ============================================================
    // TOKENIZATION
    // ============================================================

    private List<String> tokenizeList(
            String text) {

        List<String> words =
                new ArrayList<>();

        String normalized =
                normalize(text);

        if (normalized.isBlank()) {
            return words;
        }

        for (String raw :
                normalized.split("\\s+")) {

            if (raw.matches(
                    "-?\\d+(\\.\\d+)?")) {

                continue;
            }

            if (raw.length() >= 3
                    && !STOP_WORDS.contains(raw)) {

                words.add(
                        stem(raw)
                );
            }
        }

        return words;
    }

    // ============================================================
    // STEMMING
    // ============================================================

    private String stem(
            String word) {

        if (word.endsWith("ies")
                && word.length() > 4) {

            return word.substring(
                    0,
                    word.length() - 3
            ) + "y";
        }

        if (word.endsWith("ing")
                && word.length() > 5) {

            return word.substring(
                    0,
                    word.length() - 3
            );
        }

        if (word.endsWith("ed")
                && word.length() > 4) {

            return word.substring(
                    0,
                    word.length() - 2
            );
        }

        if (word.endsWith("s")
                && word.length() > 4) {

            return word.substring(
                    0,
                    word.length() - 1
            );
        }

        return word;
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    private String normalize(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9.%\\- ]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // ============================================================
    // NUMBER COMPARISON
    // ============================================================

    private Comparison compareNumbers(
            String claim,
            String evidence) {

        List<NumberFact> claimNumbers =
                extractNumbers(claim);

        List<NumberFact> evidenceNumbers =
                extractNumbers(evidence);

        if (claimNumbers.isEmpty()) {

            return new Comparison(
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        boolean relevantNumberFound =
                false;

        boolean unrelatedNumberFound =
                false;

        boolean supports =
                false;

        boolean contradicts =
                false;

        boolean comparableClaimNumberFound =
                false;

        for (NumberFact claimNumber :
                claimNumbers) {

            if (claimNumber.year()) {
                continue;
            }

            comparableClaimNumberFound = true;

            for (NumberFact evidenceNumber :
                    evidenceNumbers) {

                if (evidenceNumber.year()) {
                    continue;
                }

                if (!unitsCompatible(
                        claimNumber.unit(),
                        evidenceNumber.unit())) {

                    continue;
                }

                if (!sameNumberContext(
                        claimNumber,
                        evidenceNumber)) {

                    unrelatedNumberFound = true;

                    continue;
                }

                relevantNumberFound = true;

                if (evidenceNumber.negated()) {

                    contradicts = true;
                    continue;
                }

                double difference =
                        Math.abs(
                                claimNumber.value()
                                        - evidenceNumber.value()
                        );

                double tolerance =
                        calculateTolerance(
                                claimNumber.unit(),
                                claimNumber.value()
                        );

                if (difference <= tolerance) {

                    supports = true;

                } else {

                    contradicts = true;
                }
            }
        }

        /*
         * Matching evidence wins over contradiction
         * when the same claim has multiple close values.
         */

        if (supports) {
            contradicts = false;
        }

        return new Comparison(
                supports,
                contradicts,
                relevantNumberFound,
                unrelatedNumberFound,
                comparableClaimNumberFound
        );
    }

    private boolean unitsCompatible(
            String claimUnit,
            String evidenceUnit) {

        if (Objects.equals(
                claimUnit,
                evidenceUnit)) {

            return true;
        }

        return ("%".equals(claimUnit)
                && "number".equals(evidenceUnit))
                || ("number".equals(claimUnit)
                && "%".equals(evidenceUnit));
    }

    // ============================================================
    // NUMBER CONTEXT
    // ============================================================

    private boolean sameNumberContext(
            NumberFact claimNumber,
            NumberFact evidenceNumber) {

        String claimContext = claimNumber.context();
        String evidenceContext = evidenceNumber.context();

        if (claimContext.equals(evidenceContext)) {
            return true;
        }

        if ("GDP_GROWTH".equals(claimContext) && "GDP_GROWTH".equals(evidenceContext)) {
            return true;
        }

        if ("GDP_SHARE".equals(claimContext) && "GDP_SHARE".equals(evidenceContext)) {
            return true;
        }

        if (claimNumber.unit() != null && claimNumber.unit().equals(evidenceNumber.unit())) {
            return true;
        }

        return false;
    }

    // ============================================================
    // EXTRACT NUMBERS
    // ============================================================

    private List<NumberFact> extractNumbers(
            String text) {

        List<NumberFact> facts =
                new ArrayList<>();

        if (text == null
                || text.isBlank()) {

            return facts;
        }

        Set<Double> years =
                new HashSet<>();

        Matcher yearMatcher =
                YEAR_PATTERN.matcher(text);

        while (yearMatcher.find()) {

            try {

                years.add(
                        Double.parseDouble(
                                yearMatcher.group(1)
                        )
                );

            } catch (NumberFormatException ignored) {
            }
        }

        Matcher matcher =
                NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                matcher.group(1)
                        );

                String unit =
                        normalizeUnit(
                                matcher.group(2)
                        );

                boolean isYear =
                        years.contains(value);

                String context =
                        detectNumberContext(
                                text,
                                matcher.start(),
                                matcher.end()
                        );

                boolean negated =
                        isNegatedNumber(
                                text,
                                matcher.start()
                        );

                facts.add(
                        new NumberFact(
                                value,
                                unit,
                                isYear,
                                context,
                                negated
                        )
                );

            } catch (NumberFormatException ignored) {
            }
        }

        return facts;
    }

    private boolean isNegatedNumber(
            String text,
            int numberStart) {

        if (text == null
                || numberStart <= 0) {

            return false;
        }

        int start =
                Math.max(
                        0,
                        numberStart - 24
                );

        String beforeNumber =
                normalize(
                        text.substring(
                                start,
                                numberStart
                        )
                );

        return beforeNumber.matches(
                ".*\\b(no|not|never|without|false|incorrect|wrong)\\b\\s*$"
        );
    }

    // ============================================================
    // NUMBER UNIT
    // ============================================================

    private String normalizeUnit(
            String rawUnit) {

        if (rawUnit == null
                || rawUnit.isBlank()) {

            return "number";
        }

        String unit =
                rawUnit.toLowerCase(
                        Locale.ROOT
                );

        return switch (unit) {

            case "percent",
                 "percentage" -> "%";

            case "crore",
                 "crores" -> "crore";

            case "lakh",
                 "lakhs" -> "lakh";

            case "million",
                 "millions" -> "million";

            case "billion",
                 "billions" -> "billion";

            case "trillion",
                 "trillions" -> "trillion";

            case "cent",
                 "cents" -> "cent";

            default -> unit;
        };
    }

    // ============================================================
    // NUMBER CONTEXT
    // ============================================================

    private String detectNumberContext(
            String text,
            int numberStart,
            int numberEnd) {

        if (text == null) {
            return "UNKNOWN";
        }

        int start =
                Math.max(
                        0,
                        numberStart - 60
                );

        int end =
                Math.min(
                        text.length(),
                        numberEnd + 60
                );

        String surrounding =
                text.substring(
                        start,
                        end
                );

        String normalized =
                normalize(
                        surrounding
                ).replace(
                        " ",
                        ""
                );

        // ========================================================
        // GDP SHARE
        // Check this BEFORE GDP growth.
        // ========================================================

        boolean hasGDP =
                containsAny(
                        normalized,
                        "gdp",
                        "realgdp",
                        "nominalgdp",
                        "economy",
                        "economic"
                );

        boolean hasShare =
                containsAny(
                        normalized,
                        "share",
                        "account",
                        "accounts",
                        "portion",
                        "contribution",
                        "sector"
                );

        if (hasGDP
                && hasShare) {

            return "GDP_SHARE";
        }

        if (normalized.contains(
                "ofgdp")) {

            return "GDP_SHARE";
        }

        // ========================================================
        // GDP GROWTH
        // ========================================================

        boolean hasGrowth =
                containsAny(
                        normalized,
                        "growth",
                        "grow",
                        "grew",
                        "growing",
                        "increase",
                        "increased",
                        "increasing",
                        "rise",
                        "rose",
                        "rising",
                        "expansion",
                        "expanded",
                        "expanding"
                );

        if (hasGDP
                && hasGrowth) {

            return "GDP_GROWTH";
        }

        // ========================================================
        // UNKNOWN
        // ========================================================

        return "UNKNOWN";
    }
    // ============================================================
    // STRING MATCH
    // ============================================================

    private boolean containsAny(
            String text,
            String... words) {

        for (String word :
                words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // TOLERANCE
    // ============================================================

    private double calculateTolerance(
            String unit,
            double value) {

        if ("%".equals(unit)) {
            return 0.75;
        }

        if ("cent".equals(unit)) {
            return 0.75;
        }

        return Math.max(
                0.5,
                Math.abs(value) * 0.05
        );
    }

    // ============================================================
    // SOURCE RELIABILITY
    // ============================================================

    private double calculateReliability(
            String sourceName) {

        if (sourceName == null
                || sourceName.isBlank()) {

            return 0.30;
        }

        String source =
                sourceName.toLowerCase(
                        Locale.ROOT
                );

        // --------------------------------------------------------
        // Government / official
        // --------------------------------------------------------

        if (source.contains(
                "pib.gov.in")) {

            return 0.95;
        }

        if (source.contains(
                "gov.in")) {

            return 0.90;
        }

        // --------------------------------------------------------
        // Major news
        // --------------------------------------------------------

        if (source.contains(
                "reuters.com")) {

            return 0.95;
        }

        if (source.contains(
                "apnews.com")) {

            return 0.95;
        }

        if (source.contains(
                "bbc.com")) {

            return 0.90;
        }

        if (source.contains(
                "thehindu.com")) {

            return 0.90;
        }

        if (source.contains(
                "indiatoday.in")) {

            return 0.85;
        }

        if (source.contains(
                "ndtv.com")) {

            return 0.85;
        }

        // --------------------------------------------------------
        // Fact checking
        // --------------------------------------------------------

        if (source.contains(
                "altnews.in")) {

            return 0.90;
        }

        if (source.contains(
                "boomlive.in")) {

            return 0.90;
        }

        if (source.contains(
                "factcheck.afp.com")) {

            return 0.90;
        }

        if (source.contains(
                "snopes.com")) {

            return 0.85;
        }

        if (source.contains(
                "politifact.com")) {

            return 0.85;
        }

        if (source.contains(
                "factcheck.org")) {

            return 0.85;
        }

        // --------------------------------------------------------
        // Data sources
        // --------------------------------------------------------

        if (source.contains(
                "tradingeconomics.com")) {

            return 0.80;
        }

        if (source.contains(
                "wikipedia.org")) {

            return 0.75;
        }

        // --------------------------------------------------------
        // Social media
        // --------------------------------------------------------

        if (source.contains(
                "facebook.com")) {

            return 0.40;
        }

        if (source.contains(
                "youtube.com")) {

            return 0.50;
        }

        if (source.contains(
                "x.com")
                ||
                source.contains(
                        "twitter.com")) {

            return 0.40;
        }

        return 0.50;
    }

    // ============================================================
    // EVIDENCE RANKING
    // ============================================================

    private double calculateEvidenceScore(
            Evidence evidence) {

        if (evidence == null) {
            return 0.0;
        }

        double similarity =
                evidence.getSimilarityScore();

        double reliability =
                evidence.getReliabilityScore();

        double numericalStrength =
                evidence.getNumericalStrength();

        double relationshipScore;

        if ("SUPPORTS".equalsIgnoreCase(
                evidence.getRelationship())
                || "CONTRADICTS".equalsIgnoreCase(
                evidence.getRelationship())) {

            relationshipScore = 1.0;

        } else {

            relationshipScore = 0.20;
        }

        /*
         * Evidence quality is based on:
         *
         * Semantic similarity  -> 35%
         * Source reliability   -> 30%
         * Numerical strength   -> 20%
         * Relationship quality -> 15%
         */

        double score =
                similarity * 0.35
                        + reliability * 0.30
                        + numericalStrength * 0.20
                        + relationshipScore * 0.15;

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        score
                )
        );
    }

    // ============================================================
    // PUBLIC RELIABILITY
    // ============================================================

    public double reliabilityFor(
            String sourceName) {

        return calculateReliability(
                sourceName
        );
    }

    // ============================================================
    // ROUND
    // ============================================================

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    // ============================================================
    // RECORDS
    // ============================================================

    private record NumberFact(
            double value,
            String unit,
            boolean year,
            String context,
            boolean negated) {
    }

    private record Comparison(
            boolean supports,
            boolean contradicts,
            boolean relevantNumberFound,
            boolean unrelatedNumberFound,
            boolean comparableClaimNumberFound) {
    }

    // ============================================================
// NUMERICAL STRENGTH
// ============================================================

    private double calculateNumericalStrength(
            String claim,
            String evidence) {

        if (claim == null
                || evidence == null
                || claim.isBlank()
                || evidence.isBlank()) {

            return 0.0;
        }

        List<NumberFact> claimNumbers =
                extractNumbers(claim);

        List<NumberFact> evidenceNumbers =
                extractNumbers(evidence);

        if (claimNumbers.isEmpty()
                || evidenceNumbers.isEmpty()) {

            return 0.0;
        }

        double strongestStrength = 0.0;

        for (NumberFact claimNumber :
                claimNumbers) {

            if (claimNumber.year()) {
                continue;
            }

            for (NumberFact evidenceNumber :
                    evidenceNumbers) {

                if (evidenceNumber.year()) {
                    continue;
                }

                if (!sameNumberContext(
                        claimNumber,
                        evidenceNumber)) {

                    continue;
                }

                double claimValue =
                        claimNumber.value();

                double evidenceValue =
                        evidenceNumber.value();

                double difference =
                        Math.abs(
                                claimValue
                                        - evidenceValue
                        );

                double strength;

                // Exact match
                if (difference == 0.0) {

                    strength = 1.0;

                }

                // Very small difference
                else if (difference <= 0.5) {

                    strength = 0.90;

                }

                // Small difference
                else if (difference <= 1.0) {

                    strength = 0.75;

                }

                // Moderate difference
                else if (difference <= 3.0) {

                    strength = 0.55;

                }

                // Large difference
                else if (difference <= 10.0) {

                    strength = 0.75;

                }

                // Huge difference
                else {

                    strength = 1.0;
                }

                strongestStrength =
                        Math.max(
                                strongestStrength,
                                strength
                        );
            }
        }

        return round(
                strongestStrength
        );
    }
}
