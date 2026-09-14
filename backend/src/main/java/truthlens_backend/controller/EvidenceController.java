package truthlens_backend.controller;

import org.springframework.web.bind.annotation.*;
import truthlens_backend.model.Evidence;
import truthlens_backend.service.EvidenceAnalysisService;
import truthlens_backend.service.EvidenceRetrievalService;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {
    private final EvidenceRetrievalService retrieval;
    private final EvidenceAnalysisService analysis;

    public EvidenceController(EvidenceRetrievalService retrieval, EvidenceAnalysisService analysis) {
        this.retrieval = retrieval;
        this.analysis = analysis;
    }

    @GetMapping("/search")
    public List<Evidence> searchEvidence(@RequestParam String claim) {
        return analysis.analyzeEvidence(claim, retrieval.searchEvidence(claim));
    }
}
