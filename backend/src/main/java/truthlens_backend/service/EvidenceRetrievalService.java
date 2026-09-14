package truthlens_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import truthlens_backend.model.Evidence;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class EvidenceRetrievalService {

    // ============================================================
    // CONFIGURATION
    // ============================================================

    @Value("${serper.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    // Maximum number of evidence items returned
    private static final int MAX_RESULTS = 10;

    // ============================================================
    // MAIN SEARCH METHOD
    // ============================================================

    public List<Evidence> searchEvidence(
            String claim) {

        if (claim == null
                || claim.isBlank()) {

            return List.of();
        }

        if (apiKey == null
                || apiKey.isBlank()) {

            System.err.println(
                    "SERPER_API_KEY is missing."
            );

            return List.of();
        }

        /*
         * We perform multiple searches instead of
         * relying on one generic search.
         */

        List<String> queries =
                buildSearchQueries(claim);

        List<Evidence> allEvidence =
                new ArrayList<>();

        Set<String> seenUrls =
                new HashSet<>();

        // --------------------------------------------------------
        // Execute all searches
        // --------------------------------------------------------

        for (String query : queries) {

            List<Evidence> results =
                    performSerperSearch(query);

            for (Evidence evidence : results) {

                if (evidence == null) {
                    continue;
                }

                String url =
                        evidence.getUrl();

                if (url == null
                        || url.isBlank()) {

                    continue;
                }

                /*
                 * Prevent duplicate URLs from
                 * appearing multiple times.
                 */

                if (seenUrls.add(url)) {

                    allEvidence.add(
                            evidence
                    );
                }

                if (allEvidence.size()
                        >= MAX_RESULTS) {

                    break;
                }
            }

            if (allEvidence.size()
                    >= MAX_RESULTS) {

                break;
            }
        }

        return allEvidence;
    }

    // ============================================================
    // BUILD MULTIPLE SEARCH QUERIES
    // ============================================================

    private List<String> buildSearchQueries(
            String claim) {

        List<String> queries =
                new ArrayList<>();

        // --------------------------------------------------------
        // 1. Normal web search
        // --------------------------------------------------------

        queries.add(
                claim
        );

        // --------------------------------------------------------
        // 2. Fact-check search
        // --------------------------------------------------------

        queries.add(
                claim + " fact check"
        );

        // --------------------------------------------------------
        // 3. Verification search
        // --------------------------------------------------------

        queries.add(
                claim + " verified"
        );

        // --------------------------------------------------------
        // 4. India fact-check sources
        // --------------------------------------------------------

        queries.add(
                claim
                        + " fact check India"
        );

        return queries;
    }

    // ============================================================
    // SERPER SEARCH
    // ============================================================

    private List<Evidence> performSerperSearch(
            String query) {

        try {

            // ----------------------------------------------------
            // Request body
            // ----------------------------------------------------

            Map<String, Object> requestBody =
                    new HashMap<>();

            requestBody.put(
                    "q",
                    query
            );

            requestBody.put(
                    "num",
                    5
            );

            requestBody.put(
                    "gl",
                    "in"
            );

            requestBody.put(
                    "hl",
                    "en"
            );

            String body =
                    objectMapper.writeValueAsString(
                            requestBody
                    );

            // ----------------------------------------------------
            // HTTP request
            // ----------------------------------------------------

            HttpRequest request =
                    HttpRequest.newBuilder()

                            .uri(
                                    URI.create(
                                            "https://google.serper.dev/search"
                                    )
                            )

                            .header(
                                    "X-API-KEY",
                                    apiKey
                            )

                            .header(
                                    "Content-Type",
                                    "application/json"
                            )

                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(body)
                            )

                            .build();

            // ----------------------------------------------------
            // Send request
            // ----------------------------------------------------

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

            // ----------------------------------------------------
            // Check response
            // ----------------------------------------------------

            if (response.statusCode()
                    != 200) {

                System.err.println(
                        "Serper request failed. HTTP status: "
                                + response.statusCode()
                );

                return List.of();
            }

            // ----------------------------------------------------
            // Parse JSON
            // ----------------------------------------------------

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode organic =
                    root.path(
                            "organic"
                    );

            if (!organic.isArray()) {

                return List.of();
            }

            // ----------------------------------------------------
            // Convert search results
            // ----------------------------------------------------

            List<Evidence> results =
                    new ArrayList<>();

            for (JsonNode item :
                    organic) {

                String title =
                        item.path(
                                "title"
                        ).asText("");

                String url =
                        item.path(
                                "link"
                        ).asText("");

                String snippet =
                        item.path(
                                "snippet"
                        ).asText("");

                if (url.isBlank()) {
                    continue;
                }

                Evidence evidence =
                        new Evidence();

                evidence.setTitle(
                        title.isBlank()
                                ? "Untitled"
                                : decodeHtmlEntities(title)
                );

                evidence.setUrl(
                        url
                );

                evidence.setContent(
                        decodeHtmlEntities(snippet)
                );

                evidence.setSourceName(
                        extractSourceName(
                                url
                        )
                );

                // Initial values
                evidence.setSimilarityScore(
                        0.0
                );

                evidence.setReliabilityScore(
                        0.0
                );

                evidence.setRelationship(
                        "PENDING"
                );

                results.add(
                        evidence
                );
            }

            return results;

        } catch (Exception e) {

            System.err.println(
                    "Evidence retrieval failed: "
                            + e.getMessage()
            );

            return List.of();
        }
    }

    // ============================================================
    // EXTRACT DOMAIN
    // ============================================================

    private String extractSourceName(
            String url) {

        try {

            String host =
                    URI.create(
                            url
                    ).getHost();

            if (host == null
                    || host.isBlank()) {

                return "Unknown Source";
            }

            return host.replaceFirst(
                    "^www\\.",
                    ""
            );

        } catch (Exception e) {

            return "Unknown Source";
        }
    }
    private String decodeHtmlEntities(
            String text) {

        if (text == null
                || text.isBlank()) {

            return text;
        }

        return text
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ");
    }
}