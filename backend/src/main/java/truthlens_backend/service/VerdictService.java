package truthlens_backend.service;

import org.springframework.stereotype.Service;
import truthlens_backend.model.Evidence;

import java.util.List;

@Service
public class VerdictService {

    public String calculateVerdict(
            List<Evidence> evidenceList) {

        if (evidenceList == null
                || evidenceList.isEmpty()) {

            return "UNCERTAIN";
        }

        double support =
                calculateAverageScore(
                        evidenceList,
                        "SUPPORTS"
                );

        double contradict =
                calculateAverageScore(
                        evidenceList,
                        "CONTRADICTS"
                );

        int supportCount =
                countRelationship(
                        evidenceList,
                        "SUPPORTS"
                );

        int contradictCount =
                countRelationship(
                        evidenceList,
                        "CONTRADICTS"
                );

        if (supportCount == 0
                && contradictCount == 0) {

            return "UNCERTAIN";
        }

        // Pure contradiction
        if (contradictCount > 0 && supportCount == 0) {
            return "FALSE";
        }

        // Pure support
        if (supportCount > 0 && contradictCount == 0) {
            return "TRUE";
        }

        // Strong contradiction
        if (contradict > support
                && contradict >= 0.40) {

            return "FALSE";
        }

        // Strong support
        if (support > contradict
                && support >= 0.40) {

            return "TRUE";
        }

        // Multiple supporting sources
        if (supportCount >= 2
                && supportCount > contradictCount) {

            return "TRUE";
        }

        // Multiple contradicting sources
        if (contradictCount >= 2
                && contradictCount > supportCount) {

            return "FALSE";
        }

        return "UNCERTAIN";
    }


    public double calculateConfidence(
            List<Evidence> evidenceList) {

        if (evidenceList == null
                || evidenceList.isEmpty()) {

            return 0.0;
        }

        double support =
                calculateAverageScore(
                        evidenceList,
                        "SUPPORTS"
                );

        double contradict =
                calculateAverageScore(
                        evidenceList,
                        "CONTRADICTS"
                );

        int supportCount =
                countRelationship(
                        evidenceList,
                        "SUPPORTS"
                );

        int contradictCount =
                countRelationship(
                        evidenceList,
                        "CONTRADICTS"
                );

        int unclearCount =
                countRelationship(
                        evidenceList,
                        "UNCLEAR"
                );

        if (supportCount == 0
                && contradictCount == 0) {

            return 0.0;
        }


        // ========================================================
        // STRONGEST EVIDENCE
        // ========================================================

        double strongest =
                Math.max(
                        support,
                        contradict
                );


        // ========================================================
        // EVIDENCE AGREEMENT
        // ========================================================

        int decisiveEvidence =
                supportCount
                        + contradictCount;

        double agreement =
                decisiveEvidence == 0
                        ? 0.0
                        : (double) Math.max(
                        supportCount,
                        contradictCount
                ) / decisiveEvidence;


        // ========================================================
        // EVIDENCE COVERAGE
        // ========================================================

        double coverage =
                evidenceCoverage(
                        decisiveEvidence,
                        evidenceList.size()
                );


        // ========================================================
        // CONTRADICTION BALANCE
        // ========================================================

        double contradictionBalance =
                1.0;

        if (supportCount > 0
                && contradictCount > 0) {

            int totalDecisive =
                    supportCount
                            + contradictCount;

            double minorityRatio =
                    (double) Math.min(
                            supportCount,
                            contradictCount
                    ) / totalDecisive;

            contradictionBalance =
                    1.0
                            - (
                            minorityRatio
                                     * 0.25
                    );
        }


        // ========================================================
        // UNCLEAR EVIDENCE PENALTY
        // ========================================================

        double unclearPenalty =
                1.0;

        if (unclearCount > 0) {

            unclearPenalty =
                    Math.max(
                            0.85,
                            1.0
                                    - (
                                    0.03
                                            * unclearCount
                            )
                    );
        }


        // ========================================================
        // BASE CONFIDENCE
        // ========================================================

        double confidence =
                (
                        strongest * 0.55
                                + agreement * 0.30
                                + coverage * 0.15
                );


        // ========================================================
        // APPLY MIXED-EVIDENCE ADJUSTMENT
        // ========================================================

        confidence =
                confidence
                        * contradictionBalance
                        * unclearPenalty;


        // ========================================================
        // CAP CONFIDENCE
        // ========================================================

        confidence =
                Math.min(
                        0.95,
                        confidence
                );


        return round(
                Math.max(
                        0.0,
                        confidence
                )
        );
    }


    // ============================================================
    // AVERAGE EVIDENCE SCORE
    // ============================================================

    private double calculateAverageScore(
            List<Evidence> evidenceList,
            String relationship) {

        double total = 0.0;
        int count = 0;

        for (Evidence evidence :
                evidenceList) {

            if (evidence == null) {
                continue;
            }

            if (!relationship.equalsIgnoreCase(
                    evidence.getRelationship())) {

                continue;
            }

            double score =
                    evidence.getSimilarityScore()
                            * evidence.getReliabilityScore();

            total += score;

            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        return total / count;
    }


    // ============================================================
    // COUNT RELATIONSHIP
    // ============================================================

    private int countRelationship(
            List<Evidence> evidenceList,
            String relationship) {

        int count = 0;

        for (Evidence evidence :
                evidenceList) {

            if (evidence == null) {
                continue;
            }

            if (relationship.equalsIgnoreCase(
                    evidence.getRelationship())) {

                count++;
            }
        }

        return count;
    }


    // ============================================================
    // EVIDENCE COVERAGE
    // ============================================================

    private double evidenceCoverage(
            int decisiveEvidence,
            int totalEvidence) {

        if (totalEvidence == 0) {
            return 0.0;
        }

        return Math.min(
                1.0,
                (double) decisiveEvidence
                        / totalEvidence
        );
    }


    // ============================================================
    // ROUND
    // ============================================================

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}