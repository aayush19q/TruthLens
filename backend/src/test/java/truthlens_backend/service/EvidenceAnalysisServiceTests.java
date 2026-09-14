package truthlens_backend.service;

import org.junit.jupiter.api.Test;
import truthlens_backend.model.Evidence;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceAnalysisServiceTests {

    private final EvidenceAnalysisService service =
            new EvidenceAnalysisService(
                    new FixedSimilarityService(),
                    new FactConsistencyService()
            );

    @Test
    void matchingGdpGrowthNumberSupportsClaim() {

        Evidence evidence =
                evidence(
                        "India economy growth",
                        "India's economy expanded by 7.8 percent in 2025, official data showed."
                );

        List<Evidence> analyzed =
                service.analyzeEvidence(
                        "India's economy expanded by around 7.8% in 2025.",
                        new ArrayList<>(
                                List.of(evidence)
                        )
                );

        assertEquals(
                "SUPPORTS",
                analyzed.get(0).getRelationship()
        );
    }

    @Test
    void contradictoryGdpGrowthNumberOverridesSemanticSimilarity() {

        Evidence evidence =
                evidence(
                        "India economy growth",
                        "India's GDP growth was around 7.8 percent in 2025, not 75 percent."
                );

        List<Evidence> analyzed =
                service.analyzeEvidence(
                        "India's GDP growth was 75% in 2025.",
                        new ArrayList<>(
                                List.of(evidence)
                        )
                );

        assertEquals(
                "CONTRADICTS",
                analyzed.get(0).getRelationship()
        );
    }

    @Test
    void unrelatedNumberStaysUnclearDespiteSemanticSimilarity() {

        Evidence evidence =
                evidence(
                        "India population",
                        "India's population was about 1.4 billion in 2025."
                );

        List<Evidence> analyzed =
                service.analyzeEvidence(
                        "India's GDP growth was 75% in 2025.",
                        new ArrayList<>(
                                List.of(evidence)
                        )
                );

        assertEquals(
                "UNCLEAR",
                analyzed.get(0).getRelationship()
        );
    }

    private Evidence evidence(
            String title,
            String content) {

        Evidence evidence =
                new Evidence();

        evidence.setTitle(title);
        evidence.setUrl("https://example.com/evidence");
        evidence.setSourceName("reuters.com");
        evidence.setContent(content);

        return evidence;
    }

    private static class FixedSimilarityService
            extends SemanticSimilarityService {

        @Override
        public double calculateSimilarity(
                String text1,
                String text2) {

            return 0.95;
        }
    }
}
