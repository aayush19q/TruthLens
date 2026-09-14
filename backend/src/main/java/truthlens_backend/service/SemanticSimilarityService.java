package truthlens_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SemanticSimilarityService {

    @Value("${nlp.service.url:http://127.0.0.1:8000/similarity}")
    private String nlpUrl;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

    public double calculateSimilarity(
            String text1,
            String text2) {

        if (text1 == null
                || text2 == null
                || text1.isBlank()
                || text2.isBlank()) {

            return 0.0;
        }

        try {

            // ------------------------------------------------
            // Create JSON request body
            // ------------------------------------------------

            String requestBody =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "text1",
                                    text1,
                                    "text2",
                                    text2
                            )
                    );

            // ------------------------------------------------
            // Create HTTP request
            // ------------------------------------------------

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            nlpUrl != null && !nlpUrl.isBlank()
                                                    ? nlpUrl
                                                    : "http://127.0.0.1:8000/similarity"
                                    )
                            )
                            .version(
                                    HttpClient.Version.HTTP_1_1
                            )
                            .header(
                                    "Content-Type",
                                    "application/json; charset=UTF-8"
                            )
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            requestBody,
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            // ------------------------------------------------
            // Send request
            // ------------------------------------------------

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            // ------------------------------------------------
            // Handle response
            // ------------------------------------------------

            if (response.statusCode() != 200) {

                System.err.println(
                        "NLP service returned HTTP "
                                + response.statusCode()
                );

                return 0.0;
            }

            // ------------------------------------------------
            // Parse response JSON
            // ------------------------------------------------

            JsonNode json =
                    objectMapper.readTree(
                            response.body()
                    );

            double similarity =
                    json.path(
                            "similarity"
                    ).asDouble(0.0);

            return Math.max(
                    0.0,
                    Math.min(
                            1.0,
                            similarity
                    )
            );

        } catch (Exception e) {

            System.err.println(
                    "NLP service unavailable: "
                            + e.getMessage()
            );

            return 0.0;
        }
    }
}