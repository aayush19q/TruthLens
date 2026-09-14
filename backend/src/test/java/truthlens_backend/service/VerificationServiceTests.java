package truthlens_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import truthlens_backend.dto.VerificationRequest;
import truthlens_backend.model.Evidence;
import truthlens_backend.model.Verification;
import truthlens_backend.repository.VerificationRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationServiceTests {

    private VerificationRepository repository;
    private ClaimExtractionService claimExtractionService;
    private EvidenceRetrievalService evidenceRetrievalService;
    private EvidenceAnalysisService evidenceAnalysisService;
    private VerdictService verdictService;
    private ExplanationService explanationService;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        repository = mock(VerificationRepository.class);
        claimExtractionService = new ClaimExtractionService();
        evidenceRetrievalService = mock(EvidenceRetrievalService.class);

        SemanticSimilarityService semanticSimilarityService = new SemanticSimilarityService() {
            @Override
            public double calculateSimilarity(String text1, String text2) {
                return 0.88;
            }
        };

        FactConsistencyService factConsistencyService = new FactConsistencyService();
        evidenceAnalysisService = new EvidenceAnalysisService(semanticSimilarityService, factConsistencyService);
        verdictService = new VerdictService();
        explanationService = new ExplanationService();

        verificationService = new VerificationService(
                repository,
                claimExtractionService,
                evidenceRetrievalService,
                evidenceAnalysisService,
                verdictService,
                explanationService
        );

        when(repository.save(any(Verification.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testSingleTrueClaimVerification() {
        String text = "India's economy expanded by around 7.8% in 2025.";

        Evidence mockEvidence = new Evidence();
        mockEvidence.setTitle("India GDP Report");
        mockEvidence.setUrl("https://reuters.com/article/india-gdp");
        mockEvidence.setSourceName("reuters.com");
        mockEvidence.setContent("Official data shows India's GDP growth was 7.8 percent in 2025.");

        when(evidenceRetrievalService.searchEvidence(anyString()))
                .thenReturn(new ArrayList<>(List.of(mockEvidence)));

        VerificationRequest request = new VerificationRequest();
        request.setOriginalText(text);

        Verification result = verificationService.createVerification(request);

        assertEquals("TRUE", result.getStatus());
        assertTrue(result.getConfidence() >= 0.45);
        assertEquals(1, result.getClaims().size());
        assertEquals("TRUE", result.getClaims().get(0).getVerdict());
    }

    @Test
    void testSingleFalseClaimVerification() {
        String text = "India's GDP growth was 75% in 2025.";

        Evidence mockEvidence = new Evidence();
        mockEvidence.setTitle("India Economy 2025");
        mockEvidence.setUrl("https://reuters.com/article/india-economy");
        mockEvidence.setSourceName("reuters.com");
        mockEvidence.setContent("India's GDP growth was 7.8% in 2025, according to official reports.");

        when(evidenceRetrievalService.searchEvidence(anyString()))
                .thenReturn(new ArrayList<>(List.of(mockEvidence)));

        VerificationRequest request = new VerificationRequest();
        request.setOriginalText(text);

        Verification result = verificationService.createVerification(request);

        assertEquals("FALSE", result.getStatus());
        assertTrue(result.getConfidence() >= 0.45);
        assertEquals(1, result.getClaims().size());
        assertEquals("FALSE", result.getClaims().get(0).getVerdict());
    }

    @Test
    void testUnclearUnrelatedNumberVerification() {
        String text = "India's GDP growth was 75% in 2025.";

        Evidence mockEvidence = new Evidence();
        mockEvidence.setTitle("India Population");
        mockEvidence.setUrl("https://reuters.com/article/india-population");
        mockEvidence.setSourceName("reuters.com");
        mockEvidence.setContent("India's population was about 1.4 billion in 2025.");

        when(evidenceRetrievalService.searchEvidence(anyString()))
                .thenReturn(new ArrayList<>(List.of(mockEvidence)));

        VerificationRequest request = new VerificationRequest();
        request.setOriginalText(text);

        Verification result = verificationService.createVerification(request);

        assertEquals("UNCERTAIN", result.getStatus());
        assertEquals(1, result.getClaims().size());
        assertEquals("UNCLEAR", result.getClaims().get(0).getEvidence().get(0).getRelationship());
    }

    @Test
    void testMultipleClaimsMixedVerification() {
        String text = "India's economy expanded by around 7.8% in 2025. India's GDP growth was 75% in 2025.";

        Evidence mockEvidence = new Evidence();
        mockEvidence.setTitle("India GDP Report");
        mockEvidence.setUrl("https://reuters.com/article/india-gdp");
        mockEvidence.setSourceName("reuters.com");
        mockEvidence.setContent("Official figures confirm India's GDP growth was 7.8% in 2025.");

        when(evidenceRetrievalService.searchEvidence(anyString()))
                .thenReturn(new ArrayList<>(List.of(mockEvidence)));

        VerificationRequest request = new VerificationRequest();
        request.setOriginalText(text);

        Verification result = verificationService.createVerification(request);

        assertEquals("MIXED", result.getStatus());
        assertEquals(2, result.getClaims().size());
        assertEquals("TRUE", result.getClaims().get(0).getVerdict());
        assertEquals("FALSE", result.getClaims().get(1).getVerdict());
        assertTrue(result.getExplanation().contains("Claim 1 is supported by the available evidence"));
        assertTrue(result.getExplanation().contains("Claim 2 is contradicted by the available evidence"));
        assertTrue(result.getExplanation().contains("Overall verdict: MIXED"));
    }

    @Test
    void testHistoryRetrieval() {
        Verification v1 = new Verification();
        v1.setOriginalText("Sample claim 1");
        v1.setStatus("TRUE");

        when(repository.findAll()).thenReturn(List.of(v1));

        List<Verification> history = verificationService.getAllVerifications();

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Sample claim 1", history.get(0).getOriginalText());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testNlpServiceFailureFallback() {
        SemanticSimilarityService failingNlpService = new SemanticSimilarityService() {
            @Override
            public double calculateSimilarity(String text1, String text2) {
                return 0.0; // Simulated NLP service failure/fallback
            }
        };

        EvidenceAnalysisService fallbackAnalysisService = new EvidenceAnalysisService(
                failingNlpService,
                new FactConsistencyService()
        );

        VerificationService fallbackVerificationService = new VerificationService(
                repository,
                claimExtractionService,
                evidenceRetrievalService,
                fallbackAnalysisService,
                verdictService,
                explanationService
        );

        Evidence mockEvidence = new Evidence();
        mockEvidence.setTitle("India Economy 2025");
        mockEvidence.setUrl("https://reuters.com/article/india-economy");
        mockEvidence.setSourceName("reuters.com");
        mockEvidence.setContent("India's GDP growth was 7.8% in 2025, according to official reports.");

        when(evidenceRetrievalService.searchEvidence(anyString()))
                .thenReturn(new ArrayList<>(List.of(mockEvidence)));

        VerificationRequest request = new VerificationRequest();
        request.setOriginalText("India's GDP growth was 75% in 2025.");

        Verification result = fallbackVerificationService.createVerification(request);

        // Even with NLP service offline, numerical contradiction detection must still identify FALSE
        assertEquals("FALSE", result.getStatus());
    }
}
