package truthlens_backend.service;

import org.springframework.stereotype.Service;
import truthlens_backend.dto.VerificationRequest;
import truthlens_backend.model.Claim;
import truthlens_backend.model.Evidence;
import truthlens_backend.model.Verification;
import truthlens_backend.repository.VerificationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VerificationService {

    private final VerificationRepository repository;

    private final ClaimExtractionService claimExtractionService;

    private final EvidenceRetrievalService evidenceRetrievalService;

    private final EvidenceAnalysisService evidenceAnalysisService;

    private final VerdictService verdictService;

    private final ExplanationService explanationService;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public VerificationService(
            VerificationRepository repository,
            ClaimExtractionService claimExtractionService,
            EvidenceRetrievalService evidenceRetrievalService,
            EvidenceAnalysisService evidenceAnalysisService,
            VerdictService verdictService,
            ExplanationService explanationService) {

        this.repository =
                repository;

        this.claimExtractionService =
                claimExtractionService;

        this.evidenceRetrievalService =
                evidenceRetrievalService;

        this.evidenceAnalysisService =
                evidenceAnalysisService;

        this.verdictService =
                verdictService;

        this.explanationService =
                explanationService;
    }

    // ============================================================
    // CREATE VERIFICATION
    // ============================================================

    public Verification createVerification(
            VerificationRequest request) {

        Verification verification =
                new Verification();

        String originalText =
                request.getOriginalText();

        verification.setOriginalText(
                originalText
        );

        verification.setCreatedAt(
                LocalDateTime.now()
        );

        verification.setStatus(
                "PROCESSING"
        );

        verification.setConfidence(
                0.0
        );

        // --------------------------------------------------------
        // Extract claims
        // --------------------------------------------------------

        List<Claim> claims =
                claimExtractionService.extractClaims(
                        originalText
                );

        if (claims == null) {

            claims =
                    new ArrayList<>();
        }

        // --------------------------------------------------------
        // Analyze every claim
        // --------------------------------------------------------

        for (Claim claim :
                claims) {

            // ----------------------------------------------------
            // Search evidence
            // ----------------------------------------------------

            List<Evidence> evidenceList =
                    evidenceRetrievalService.searchEvidence(
                            claim.getText()
                    );

            // ----------------------------------------------------
            // Analyze evidence
            // ----------------------------------------------------

            evidenceList =
                    evidenceAnalysisService.analyzeEvidence(
                            claim.getText(),
                            evidenceList
                    );

            // ----------------------------------------------------
            // Calculate verdict
            // ----------------------------------------------------

            String verdict =
                    verdictService.calculateVerdict(
                            evidenceList
                    );

            // ----------------------------------------------------
            // Calculate confidence
            // ----------------------------------------------------

            double confidence =
                    verdictService.calculateConfidence(
                            evidenceList
                    );

            // ----------------------------------------------------
            // Save result inside claim
            // ----------------------------------------------------

            claim.setEvidence(
                    evidenceList
            );

            claim.setVerdict(
                    verdict
            );

            claim.setConfidence(
                    round(confidence)
            );
        }

        // --------------------------------------------------------
        // Save claims
        // --------------------------------------------------------

        verification.setClaims(
                claims
        );

        // --------------------------------------------------------
        // Overall status
        // --------------------------------------------------------

        String overallStatus =
                calculateOverallStatus(
                        claims
                );

        verification.setStatus(
                overallStatus
        );

        // --------------------------------------------------------
        // Overall confidence
        // --------------------------------------------------------

        double overallConfidence =
                calculateOverallConfidence(
                        claims
                );

        verification.setConfidence(
                overallConfidence
        );

        // --------------------------------------------------------
        // Generate explanation
        // --------------------------------------------------------

        String explanation =
                explanationService.generateExplanation(
                        overallStatus,
                        overallConfidence,
                        claims
                );

        verification.setExplanation(
                explanation
        );

        // --------------------------------------------------------
        // Save to MongoDB
        // --------------------------------------------------------

        return repository.save(
                verification
        );
    }

    // ============================================================
    // GET ALL
    // ============================================================

    public List<Verification> getAllVerifications() {

        return repository.findAll();
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    public Verification getVerification(
            String id) {

        return repository
                .findById(id)
                .orElse(null);
    }

    // ============================================================
    // OVERALL STATUS
    // ============================================================

    private String calculateOverallStatus(List<Claim> claims) {

        if (claims == null || claims.isEmpty()) {
            return "UNCERTAIN";
        }

        int trueCount = 0;
        int falseCount = 0;
        int uncertainCount = 0;

        for (Claim claim : claims) {

            if (claim == null) {
                continue;
            }

            if ("TRUE".equalsIgnoreCase(
                    claim.getVerdict())) {

                trueCount++;

            } else if ("FALSE".equalsIgnoreCase(
                    claim.getVerdict())) {

                falseCount++;

            } else {

                uncertainCount++;
            }
        }

        // --------------------------------------------------------
        // MIXED RESULT
        // --------------------------------------------------------
        //
        // Some claims are true while others are false.
        //
        // Example:
        //
        // Claim 1 → TRUE
        // Claim 2 → FALSE
        //
        // Overall → MIXED
        // --------------------------------------------------------

        if (trueCount > 0
                && falseCount > 0) {

            return "MIXED";
        }

        // --------------------------------------------------------
        // All claims are true
        // --------------------------------------------------------

        if (trueCount > 0
                && falseCount == 0
                && uncertainCount == 0) {

            return "TRUE";
        }

        // --------------------------------------------------------
        // All claims are false
        // --------------------------------------------------------

        if (falseCount > 0
                && trueCount == 0
                && uncertainCount == 0) {

            return "FALSE";
        }

        // --------------------------------------------------------
        // Majority true
        // --------------------------------------------------------

        if (trueCount > falseCount
                && trueCount > uncertainCount) {

            return "TRUE";
        }

        // --------------------------------------------------------
        // Majority false
        // --------------------------------------------------------

        if (falseCount > trueCount
                && falseCount > uncertainCount) {

            return "FALSE";
        }

        // --------------------------------------------------------
        // Otherwise uncertain
        // --------------------------------------------------------

        return "UNCERTAIN";
    }

    // ============================================================
    // OVERALL CONFIDENCE
    // ============================================================

    private double calculateOverallConfidence(
            List<Claim> claims) {

        if (claims == null
                || claims.isEmpty()) {

            return 0.0;
        }

        double total =
                0.0;

        for (Claim claim :
                claims) {

            total +=
                    claim.getConfidence();
        }

        double average =
                total / claims.size();

        return round(
                average
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
}