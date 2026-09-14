package truthlens_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificationRequest {

    @NotBlank(message = "Original text must not be blank")
    @Size(max = 5000, message = "Original text must not exceed 5000 characters")
    private String originalText;

    public VerificationRequest() {
    }

    public VerificationRequest(String originalText) {
        this.originalText = originalText;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
}