# TruthLens Architecture & Verification Pipeline Documentation

## System Architecture

```
                    +-----------------------------+
                    |     React Web Frontend      |
                    |    (Vite / Port 5173)       |
                    +-----------------------------+
                                   |
                                   | HTTP REST Requests
                                   v
                    +-----------------------------+
                    |   Spring Boot Web Server    |
                    |    (Tomcat / Port 8080)     |
                    +-----------------------------+
                                   |
           +-----------------------+-----------------------+
           |                       |                       |
           v                       v                       v
+--------------------+   +--------------------+   +--------------------+
|  MongoDB Database  |   | Serper Web Search  |   | Python NLP Service |
|    (Port 27017)    |   |  (Serper.dev API)  |   | (FastAPI/Port 8000)|
+--------------------+   +--------------------+   +--------------------+
                                                           |
                                                           v
                                                  Sentence Transformer
                                                  (all-MiniLM-L6-v2)
```

---

## Detailed Verification Pipeline Flow

1. **User Text Submission**: The user submits a claim or multi-sentence news story via the React frontend or REST API.
2. **Claim Extraction (`ClaimExtractionService`)**: Splits input text into discrete atomic claims using sentence demarcation regex.
3. **Evidence Retrieval (`EvidenceRetrievalService`)**: Generates multiple search queries (direct claim, fact check, verified) and queries Serper API to retrieve organic web evidence items.
4. **Semantic Similarity (`SemanticSimilarityService`)**: Sends claim-evidence text pairs to the Python FastAPI NLP service (`http://127.0.0.1:8000/similarity`), which calculates cosine similarity using `all-MiniLM-L6-v2` dense embeddings.
5. **Numerical & Fact Consistency Analysis (`FactConsistencyService`)**: Extracts years, numbers, percentages, and financial units. Identifies numerical contradictions and temporal mismatches between claims and evidence snippets.
6. **Evidence Analysis & Relationship Classification (`EvidenceAnalysisService`)**:
   - Computes source reliability score based on domain authority (government, Reuters, BBC, fact-checkers).
   - Determines relationship:
     - `CONTRADICTS`: Numerical or fact mismatch detected (takes highest priority over semantic similarity).
     - `SUPPORTS`: Matching numerical values or strong semantic match (`similarity >= 0.50`) without contradiction.
     - `UNCLEAR`: Unrelated numbers or weak evidence.
7. **Verdict & Confidence Calculation (`VerdictService`)**: Evaluates evidence for each claim to assign a claim verdict (`TRUE`, `FALSE`, `UNCERTAIN`) and confidence score.
8. **Overall Verdict Aggregation (`VerificationService`)**:
   - `MIXED`: If at least one claim is `TRUE` and another is `FALSE`.
   - `TRUE`: If all claims are `TRUE` or decisive majority is `TRUE`.
   - `FALSE`: If all claims are `FALSE` or decisive majority is `FALSE`.
   - `UNCERTAIN`: If no decisive majority exists.
9. **Explainable Reasoning (`ExplanationService`)**: Synthesizes a structured narrative explanation describing the overall verdict and multi-claim outcomes.
10. **MongoDB Persistence**: Stores the complete verification result in the `verifications` collection for historical lookup.
