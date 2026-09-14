package truthlens_backend.model;

import java.util.ArrayList;
import java.util.List;

public class Claim {

    private String id;

    private String text;

    private String verdict;

    private double confidence;

    private List<Evidence> evidence = new ArrayList<>();

    // Empty constructor
    public Claim() {
    }

    // Parameterized constructor
    public Claim(String id, String text, String verdict, double confidence) {
        this.id = id;
        this.text = text;
        this.verdict = verdict;
        this.confidence = confidence;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<Evidence> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<Evidence> evidence) {
        this.evidence = evidence;
    }
}