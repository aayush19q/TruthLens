package truthlens_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "verifications")
public class Verification {

    @Id
    private String id;

    private String originalText;

    private String status;

    private double confidence;

    private LocalDateTime createdAt;

    private String explanation;

    private List<Claim> claims = new ArrayList<>();

    public Verification() {
    }

    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(
            String originalText) {

        this.originalText = originalText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(
            double confidence) {

        this.confidence = confidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(
            String explanation) {

        this.explanation = explanation;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(
            List<Claim> claims) {

        this.claims = claims;
    }
}