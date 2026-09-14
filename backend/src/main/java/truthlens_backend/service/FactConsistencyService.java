package truthlens_backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FactConsistencyService {

    /*
     * Detect years such as:
     *
     * 2025
     * 2026
     * 2025-26
     * 2026-27
     */
    private static final Pattern YEAR_PATTERN =
            Pattern.compile(
                    "\\b(20\\d{2})(?:[-/](\\d{2,4}))?\\b"
            );

    /*
     * Detect numbers and percentages such as:
     *
     * 75%
     * 7.8%
     * 7.8 percent
     * 75
     * 1.5
     */
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "(?<![\\w.])" +
                            "(\\d+(?:\\.\\d+)?)" +
                            "\\s*(%|percent|percentage)?",
                    Pattern.CASE_INSENSITIVE
            );

    public ConsistencyResult check(
            String claim,
            String evidence) {

        if (claim == null
                || evidence == null
                || claim.isBlank()
                || evidence.isBlank()) {

            return new ConsistencyResult(
                    "INSUFFICIENT",
                    false,
                    false,
                    List.of(),
                    List.of()
            );
        }

        List<String> claimYears =
                extractYears(claim);

        List<String> evidenceYears =
                extractYears(evidence);

        List<Double> claimNumbers =
                extractNumbers(claim, claimYears);

        List<Double> evidenceNumbers =
                extractNumbers(evidence, evidenceYears);

        // ----------------------------------------------------
        // Check temporal consistency
        // ----------------------------------------------------

        boolean yearMismatch =
                hasYearMismatch(
                        claimYears,
                        evidenceYears
                );

        // ----------------------------------------------------
        // Check numerical consistency
        // ----------------------------------------------------

        boolean numberMismatch =
                hasRelevantNumberMismatch(
                        claim,
                        evidence,
                        claimNumbers,
                        evidenceNumbers
                );

        // ----------------------------------------------------
        // Determine result
        // ----------------------------------------------------

        if (yearMismatch
                && numberMismatch) {

            return new ConsistencyResult(
                    "TEMPORAL_AND_NUMERICAL_MISMATCH",
                    true,
                    true,
                    claimYears,
                    evidenceYears
            );
        }

        if (yearMismatch) {

            return new ConsistencyResult(
                    "TEMPORAL_MISMATCH",
                    true,
                    false,
                    claimYears,
                    evidenceYears
            );
        }

        if (numberMismatch) {

            return new ConsistencyResult(
                    "NUMERICAL_MISMATCH",
                    false,
                    true,
                    claimYears,
                    evidenceYears
            );
        }

        if (!claimYears.isEmpty()
                && !evidenceYears.isEmpty()) {

            return new ConsistencyResult(
                    "CONSISTENT",
                    false,
                    false,
                    claimYears,
                    evidenceYears
            );
        }

        return new ConsistencyResult(
                "INSUFFICIENT",
                false,
                false,
                claimYears,
                evidenceYears
        );
    }

    // ========================================================
    // YEAR EXTRACTION
    // ========================================================

    private List<String> extractYears(
            String text) {

        List<String> years =
                new ArrayList<>();

        Matcher matcher =
                YEAR_PATTERN.matcher(text);

        while (matcher.find()) {

            String firstYear =
                    matcher.group(1);

            String secondYear =
                    matcher.group(2);

            if (secondYear != null) {

                years.add(
                        firstYear
                                + "-"
                                + secondYear
                );

            } else {

                years.add(firstYear);
            }
        }

        return years;
    }

    // ========================================================
    // NUMBER EXTRACTION
    // ========================================================

    private List<Double> extractNumbers(
            String text,
            List<String> years) {

        List<Double> numbers =
                new ArrayList<>();

        Matcher matcher =
                NUMBER_PATTERN.matcher(text);

        while (matcher.find()) {

            try {

                double value =
                        Double.parseDouble(
                                matcher.group(1)
                        );

                if (isYearValue(
                        value,
                        years)) {

                    continue;
                }

                numbers.add(value);

            } catch (NumberFormatException ignored) {
                // Ignore malformed numbers
            }
        }

        return numbers;
    }

    private boolean isYearValue(
            double value,
            List<String> years) {

        if (years == null
                || years.isEmpty()) {

            return false;
        }

        int wholeNumber =
                (int) value;

        if (value != wholeNumber) {
            return false;
        }

        for (String year :
                years) {

            if (parseStartYear(year) == wholeNumber
                    || parseEndYear(year) == wholeNumber) {

                return true;
            }
        }

        return false;
    }

    // ========================================================
    // YEAR COMPARISON
    // ========================================================

    private boolean hasYearMismatch(
            List<String> claimYears,
            List<String> evidenceYears) {

        if (claimYears.isEmpty()
                || evidenceYears.isEmpty()) {

            return false;
        }

        /*
         * If at least one year/period from the claim
         * is also present in the evidence, consider
         * the time period consistent.
         */

        for (String claimYear :
                claimYears) {

            for (String evidenceYear :
                    evidenceYears) {

                if (yearsOverlap(
                        claimYear,
                        evidenceYear)) {

                    return false;
                }
            }
        }

        return true;
    }

    // ========================================================
    // YEAR OVERLAP
    // ========================================================

    private boolean yearsOverlap(
            String claimYear,
            String evidenceYear) {

        int claimStart =
                parseStartYear(
                        claimYear
                );

        int claimEnd =
                parseEndYear(
                        claimYear
                );

        int evidenceStart =
                parseStartYear(
                        evidenceYear
                );

        int evidenceEnd =
                parseEndYear(
                        evidenceYear
                );

        return claimStart <= evidenceEnd
                && evidenceStart <= claimEnd;
    }

    private int parseStartYear(
            String year) {

        try {

            return Integer.parseInt(
                    year.substring(
                            0,
                            4
                    )
            );

        } catch (Exception e) {

            return -1;
        }
    }

    private int parseEndYear(
            String year) {

        try {

            if (!year.contains("-")) {

                return parseStartYear(
                        year
                );
            }

            String[] parts =
                    year.split("-");

            int start =
                    Integer.parseInt(
                            parts[0]
                    );

            String endPart =
                    parts[1];

            int end;

            if (endPart.length() == 2) {

                end =
                        (start / 100) * 100
                                + Integer.parseInt(
                                endPart
                        );

                /*
                 * Handle 2025-26 correctly.
                 */
                if (end < start) {
                    end += 100;
                }

            } else {

                end =
                        Integer.parseInt(
                                endPart
                        );
            }

            return end;

        } catch (Exception e) {

            return parseStartYear(
                    year
            );
        }
    }

    // ========================================================
    // NUMBER COMPARISON
    // ========================================================

    private boolean hasRelevantNumberMismatch(
            String claim,
            String evidence,
            List<Double> claimNumbers,
            List<Double> evidenceNumbers) {

        if (claimNumbers.isEmpty()
                || evidenceNumbers.isEmpty()) {

            return false;
        }

        /*
         * Only compare numbers when the claim and
         * evidence appear to discuss the same topic.
         */

        String claimNormalized =
                normalize(claim);

        String evidenceNormalized =
                normalize(evidence);

        boolean sameTopic =
                containsTopicWord(
                        claimNormalized,
                        evidenceNormalized
                );

        if (!sameTopic) {
            return false;
        }

        /*
         * If any number from the claim is close to
         * a number in the evidence, consider it
         * numerically consistent.
         */

        for (Double claimNumber :
                claimNumbers) {

            for (Double evidenceNumber :
                    evidenceNumbers) {

                if (numbersAreSimilar(
                        claimNumber,
                        evidenceNumber
                )) {

                    return false;
                }
            }
        }

        return true;
    }

    // ========================================================
    // TOPIC CHECK
    // ========================================================

    private boolean containsTopicWord(
            String claim,
            String evidence) {

        List<Set<String>> topicGroups = List.of(
                Set.of("gdp", "growth", "grow", "grew", "growing", "expand", "expanded", "expanding", "expansion", "economy", "economic", "increase", "increased"),
                Set.of("price", "prices", "cost", "costs", "rate", "rates", "fuel", "petrol", "diesel"),
                Set.of("inflation", "cpi", "consumer"),
                Set.of("population", "people", "citizens"),
                Set.of("employment", "unemployment", "job", "jobs", "workers", "workforce"),
                Set.of("revenue", "income", "profit", "exports", "imports", "production", "investment"),
                Set.of("percent", "percentage", "%")
        );

        for (Set<String> group : topicGroups) {
            boolean claimHasGroup = false;
            boolean evidenceHasGroup = false;

            for (String word : group) {
                if (claim.contains(word)) {
                    claimHasGroup = true;
                }
                if (evidence.contains(word)) {
                    evidenceHasGroup = true;
                }
            }

            if (claimHasGroup && evidenceHasGroup) {
                return true;
            }
        }

        String[] topics = {
                "gdp",
                "growth",
                "economy",
                "economic",
                "inflation",
                "population",
                "revenue",
                "income",
                "unemployment",
                "exports",
                "imports",
                "production",
                "investment",
                "price",
                "percentage",
                "percent"
        };

        for (String topic :
                topics) {

            if (claim.contains(topic)
                    && evidence.contains(topic)) {

                return true;
            }
        }

        return false;
    }

    // ========================================================
    // NUMBERS ARE SIMILAR
    // ========================================================

    private boolean numbersAreSimilar(
            double first,
            double second) {

        double difference =
                Math.abs(
                        first - second
                );

        /*
         * Exact / near-exact percentage match.
         *
         * Example:
         *
         * 7.8 vs 7.8
         * 7.8 vs 7.77
         */

        if (difference <= 0.15) {
            return true;
        }

        /*
         * Handle small rounding differences.
         */

        if (first != 0) {

            double relativeDifference =
                    difference
                            / Math.abs(first);

            return relativeDifference <= 0.03;
        }

        return false;
    }

    // ========================================================
    // NORMALIZE
    // ========================================================

    private String normalize(
            String text) {

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9% ]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // ========================================================
    // RESULT RECORD
    // ========================================================

    public record ConsistencyResult(
            String status,
            boolean temporalMismatch,
            boolean numericalMismatch,
            List<String> claimYears,
            List<String> evidenceYears
    ) {
    }
}
