package truthlens_backend.service;

import org.junit.jupiter.api.Test;
import truthlens_backend.model.Claim;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationServiceTests {

    private final ExplanationService service =
            new ExplanationService();

    @Test
    void mixedResultExplainsMultipleClaimOutcomes() {

        Claim supported =
                claim(
                        "India's economy expanded by around 7.8% in 2025.",
                        "TRUE"
                );

        Claim contradicted =
                claim(
                        "India's GDP growth was 75% in 2025.",
                        "FALSE"
                );

        String explanation =
                service.generateExplanation(
                        "MIXED",
                        0.72,
                        List.of(
                                supported,
                                contradicted
                        )
                );

        assertTrue(
                explanation.contains(
                        "Claim 1 is supported by the available evidence"
                )
        );

        assertTrue(
                explanation.contains(
                        "Claim 2 is contradicted by the available evidence"
                )
        );

        assertTrue(
                explanation.contains(
                        "Overall verdict: MIXED"
                )
        );
    }

    private Claim claim(
            String text,
            String verdict) {

        Claim claim =
                new Claim();

        claim.setText(text);
        claim.setVerdict(verdict);
        claim.setConfidence(0.8);

        return claim;
    }
}
