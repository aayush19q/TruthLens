package truthlens_backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import truthlens_backend.dto.VerificationRequest;
import truthlens_backend.model.Verification;
import truthlens_backend.service.VerificationService;

import java.util.List;

@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    // ---------------------------------------------------------
    // CREATE VERIFICATION
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<Verification> createVerification(
            @Valid @RequestBody VerificationRequest request) {

        Verification verification =
                service.createVerification(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(verification);
    }

    // ---------------------------------------------------------
    // GET ALL VERIFICATIONS
    // ---------------------------------------------------------

    @GetMapping
    public List<Verification> getAllVerifications() {

        return service.getAllVerifications();
    }

    // ---------------------------------------------------------
    // GET VERIFICATION BY ID
    // ---------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<Verification> getVerification(
            @PathVariable String id) {

        Verification result =
                service.getVerification(id);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }
}