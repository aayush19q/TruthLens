package truthlens_backend.controller;

import org.springframework.web.bind.annotation.*;
import truthlens_backend.model.Evidence;
import truthlens_backend.service.EvidenceAnalysisService;
import truthlens_backend.service.EvidenceRetrievalService;
import truthlens_backend.service.VerdictService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VerificationAnalysisController {
    private final EvidenceRetrievalService retrieval;
    private final EvidenceAnalysisService analysis;
    private final VerdictService verdict;

    public VerificationAnalysisController(EvidenceRetrievalService retrieval, EvidenceAnalysisService analysis, VerdictService verdict) {
        this.retrieval = retrieval;
        this.analysis = analysis;
        this.verdict = verdict;
    }

    @GetMapping("/verify")
    public Map<String, Object> verifyClaim(@RequestParam String claim) {
        List<Evidence> evidence = analysis.analyzeEvidence(claim, retrieval.searchEvidence(claim));
        return Map.of("claim", claim, "verdict", verdict.calculateVerdict(evidence),
                "confidence", verdict.calculateConfidence(evidence), "evidence", evidence);
    }
}
