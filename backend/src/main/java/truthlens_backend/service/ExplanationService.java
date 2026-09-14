package truthlens_backend.service;

import org.springframework.stereotype.Service;
import truthlens_backend.model.Claim;
import truthlens_backend.model.Evidence;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExplanationService {

    public String generateExplanation(
            String status,
            double confidence,
            List<Claim> claims) {

        if (claims == null || claims.isEmpty()) {
            return "No claims were available for verification.";
        }

        int totalClaims = claims.size();

        int trueClaims = 0;
        int falseClaims = 0;
        int uncertainClaims = 0;

        int supportingEvidence = 0;
        int contradictingEvidence = 0;
        int unclearEvidence = 0;

        Evidence strongestEvidence = null;
        Claim strongestClaim = null;

        double strongestScore = 0.0;

        for (Claim claim : claims) {

            if (claim == null) {
                continue;
            }

            // --------------------------------------------------
            // Count claim verdicts
            // --------------------------------------------------

            if ("TRUE".equalsIgnoreCase(
                    claim.getVerdict())) {

                trueClaims++;

            } else if ("FALSE".equalsIgnoreCase(
                    claim.getVerdict())) {

                falseClaims++;

            } else {

                uncertainClaims++;
            }

            List<Evidence> evidenceList =
                    claim.getEvidence();

            if (evidenceList == null) {
                continue;
            }

            // --------------------------------------------------
            // Analyze evidence
            // --------------------------------------------------

            for (Evidence evidence : evidenceList) {

                if (evidence == null) {
                    continue;
                }

                String relationship =
                        evidence.getRelationship();

                if ("SUPPORTS".equalsIgnoreCase(
                        relationship)) {

                    supportingEvidence++;

                } else if ("CONTRADICTS".equalsIgnoreCase(
                        relationship)) {

                    contradictingEvidence++;

                } else {

                    unclearEvidence++;
                }

                // --------------------------------------------------
                // Find strongest evidence
                // --------------------------------------------------

                double score =
                        evidence.getSimilarityScore()
                                * evidence.getReliabilityScore();

                if (score > strongestScore) {

                    strongestScore = score;

                    strongestEvidence =
                            evidence;

                    strongestClaim =
                            claim;
                }
            }
        }

        List<String> points =
                new ArrayList<>();

        // --------------------------------------------------
        // Overall verdict explanation
        // --------------------------------------------------

        if ("TRUE".equalsIgnoreCase(status)) {

            points.add(
                    "The available evidence generally supports the submitted claim."
            );

        } else if ("FALSE".equalsIgnoreCase(status)) {

            points.add(
                    "The available evidence generally contradicts the submitted claim."
            );

        } else if ("MIXED".equalsIgnoreCase(status)) {

            points.add(
                    "The submitted text contains claims with different verification outcomes. Some claims are supported by the available evidence while others are contradicted."
            );

        } else {

            points.add(
                    "The available evidence is mixed or insufficient to reach a confident conclusion."
            );
        }

        // --------------------------------------------------
        // Claim-specific explanation
        // --------------------------------------------------

        if (totalClaims > 1) {

            List<String> claimOutcomes =
                    new ArrayList<>();

            int claimNumber =
                    1;

            for (Claim claim :
                    claims) {

                if (claim == null) {
                    continue;
                }

                String verdict =
                        claim.getVerdict() == null
                                ? "UNCERTAIN"
                                : claim.getVerdict();

                String outcome =
                        switch (verdict.toUpperCase()) {

                            case "TRUE" -> "supported by the available evidence";

                            case "FALSE" -> "contradicted by the available evidence";

                            default -> "uncertain based on the available evidence";
                        };

                claimOutcomes.add(
                        "Claim "
                                + claimNumber
                                + " is "
                                + outcome
                );

                claimNumber++;
            }

            if (!claimOutcomes.isEmpty()) {
                String claimsSummary;
                if (claimOutcomes.size() == 2) {
                    claimsSummary = claimOutcomes.get(0) + ", while " + claimOutcomes.get(1);
                } else if (claimOutcomes.size() > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < claimOutcomes.size(); i++) {
                        if (i > 0) {
                            if (i == claimOutcomes.size() - 1) {
                                sb.append(", and ");
                            } else {
                                sb.append(", ");
                            }
                        }
                        sb.append(claimOutcomes.get(i));
                    }
                    claimsSummary = sb.toString();
                } else {
                    claimsSummary = claimOutcomes.get(0);
                }
                points.add(
                        claimsSummary
                                + ". Overall verdict: "
                                + status
                                + "."
                );
            }

        } else if (strongestClaim != null
                && strongestClaim.getText() != null) {

            points.add(
                    "Claim analyzed: \""
                            + strongestClaim.getText()
                            + "\""
            );
        }

        // --------------------------------------------------
        // Supporting evidence
        // --------------------------------------------------

        if (supportingEvidence > 0) {

            points.add(
                    supportingEvidence
                            + " evidence source"
                            + (supportingEvidence == 1
                            ? ""
                            : "s")
                            + " support"
                            + (supportingEvidence == 1
                            ? "s"
                            : "")
                            + " the claim."
            );
        }

        // --------------------------------------------------
        // Contradicting evidence
        // --------------------------------------------------

        if (contradictingEvidence > 0) {

            points.add(
                    contradictingEvidence
                            + " evidence source"
                            + (contradictingEvidence == 1
                            ? ""
                            : "s")
                            + " contradict"
                            + (contradictingEvidence == 1
                            ? "s"
                            : "")
                            + " the claim."
            );
        }

        // --------------------------------------------------
        // Unclear evidence
        // --------------------------------------------------

        if (unclearEvidence > 0) {

            points.add(
                    unclearEvidence
                            + " source"
                            + (unclearEvidence == 1
                            ? ""
                            : "s")
                            + " was classified as unclear because it could not be directly matched to the claim."
            );
        }

        // --------------------------------------------------
        // Strongest evidence details
        // --------------------------------------------------

        if (strongestEvidence != null) {

            String source =
                    strongestEvidence.getSourceName();

            String content =
                    strongestEvidence.getContent();

            double similarity =
                    strongestEvidence
                            .getSimilarityScore();

            double reliability =
                    strongestEvidence
                            .getReliabilityScore();

            if (source != null
                    && !source.isBlank()) {

                points.add(
                        "The strongest matched evidence came from "
                                + source
                                + "."
                );
            }

            if (content != null
                    && !content.isBlank()) {

                String cleanedContent =
                        content.trim();

                /*
                 * Keep the explanation concise.
                 * Long search snippets should not
                 * make the explanation huge.
                 */

                if (cleanedContent.length() > 220) {

                    cleanedContent =
                            cleanedContent.substring(
                                    0,
                                    220
                            )
                                    + "...";
                }

                points.add(
                        "Evidence summary: \""
                                + cleanedContent
                                + "\""
                );
            }

            points.add(
                    "Semantic similarity was "
                            + Math.round(
                            similarity * 100
                    )
                            + "% and source reliability was "
                            + Math.round(
                            reliability * 100
                    )
                            + "%."
            );
        }

        // --------------------------------------------------
        // Multiple claims
        // --------------------------------------------------

        if (totalClaims > 1) {

            points.add(
                    totalClaims
                            + " claims were analyzed separately: "
                            + trueClaims
                            + " supported, "
                            + falseClaims
                            + " contradicted, and "
                            + uncertainClaims
                            + " uncertain."
            );
        }

        // --------------------------------------------------
        // Overall confidence
        // --------------------------------------------------

        int confidencePercent =
                (int) Math.round(
                        confidence * 100
                );

        points.add(
                "Overall confidence is "
                        + confidencePercent
                        + "%."
        );

        return String.join(
                " ",
                points
        );
    }
}
