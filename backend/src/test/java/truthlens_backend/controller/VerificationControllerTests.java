package truthlens_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import truthlens_backend.dto.VerificationRequest;
import truthlens_backend.exception.GlobalExceptionHandler;
import truthlens_backend.model.Verification;
import truthlens_backend.service.VerificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VerificationControllerTests {

    private MockMvc mockMvc;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = mock(VerificationService.class);
        VerificationController controller = new VerificationController(verificationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testBlankVerificationRequestValidationFailure() throws Exception {
        String invalidJson = "{\"originalText\": \"   \"}";

        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.originalText").exists());
    }

    @Test
    void testValidVerificationRequestSuccess() throws Exception {
        Verification mockVerification = new Verification();
        mockVerification.setOriginalText("Valid claim test");
        mockVerification.setStatus("TRUE");
        mockVerification.setConfidence(0.9);

        when(verificationService.createVerification(any(VerificationRequest.class)))
                .thenReturn(mockVerification);

        String validJson = "{\"originalText\": \"Valid claim test\"}";

        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TRUE"))
                .andExpect(jsonPath("$.originalText").value("Valid claim test"));
    }
}
