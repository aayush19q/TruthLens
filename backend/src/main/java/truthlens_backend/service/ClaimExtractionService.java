package truthlens_backend.service;

import org.springframework.stereotype.Service;
import truthlens_backend.model.Claim;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimExtractionService {

    public List<Claim> extractClaims(String text) {

        List<Claim> claims = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return claims;
        }

        // Split the text into sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");

        for (String sentence : sentences) {

            sentence = sentence.trim();

            if (!sentence.isEmpty()) {

                Claim claim = new Claim();

                claim.setId(UUID.randomUUID().toString());
                claim.setText(sentence);
                claim.setVerdict("PENDING");
                claim.setConfidence(0.0);

                claims.add(claim);
            }
        }

        return claims;
    }
}