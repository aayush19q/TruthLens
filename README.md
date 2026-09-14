# TruthLens: AI-Assisted Fake-News & Claim Verification System

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Backend](https://img.shields.io/badge/backend-Spring%20Boot%203-blue.svg)]()
[![NLP Service](https://img.shields.io/badge/nlp-FastAPI%20%2B%20SentenceTransformers-green.svg)]()
[![Frontend](https://img.shields.io/badge/frontend-React%20%2B%20Vite-cyan.svg)]()
[![Database](https://img.shields.io/badge/database-MongoDB-green.svg)]()

TruthLens is an AI-assisted claim extraction, evidence retrieval, semantic verification, and explainable fact-checking platform. It analyzes news stories and user claims by retrieving real-time web evidence, evaluating source reliability, computing Transformer-based semantic similarity, detecting numerical and temporal inconsistencies, and producing evidence-backed verdicts with human-readable explanations.

---

## Architecture Diagram

```
+-----------------------------------------------------------------------+
|                            React Frontend                             |
|                             (Port 5173)                               |
+-----------------------------------------------------------------------+
                                   |
                                   | HTTP REST API
                                   v
+-----------------------------------------------------------------------+
|                       Spring Boot Backend                             |
|                             (Port 8080)                               |
|                                                                       |
|  [ClaimExtraction] -> [EvidenceRetrieval] -> [EvidenceAnalysis]       |
|                             |                      |                  |
|                             |                      v                  |
|                             |           [FactConsistencyService]      |
|                             |                      |                  |
|                             |                      v                  |
|                             |         [SemanticSimilarityService]     |
|                             |                      |                  |
|                             v                      v                  |
|                      [Serper Web Search]    [Python NLP Service]      |
|                                                    (Port 8000)        |
|                                                        |              |
|                                                        v              |
|                                             [Sentence Transformer]    |
|                                             (all-MiniLM-L6-v2)        |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                          MongoDB Database                             |
|                       (Local / Port 27017)                            |
+-----------------------------------------------------------------------+
```

---

## Verification Pipeline Flow

```
   User Claim / Text
           │
           ▼
  1. Claim Extraction
           │
           ▼
  2. Evidence Retrieval (Serper Web Search)
           │
           ▼
  3. Semantic Similarity (Sentence Transformers all-MiniLM-L6-v2)
           │
           ▼
  4. Numerical & Fact Consistency Check (FactConsistencyService)
           │
           ▼
  5. Source Reliability & Evidence Ranking
           │
           ▼
  6. Relationship Classification (SUPPORTS / CONTRADICTS / UNCLEAR)
           │
           ▼
  7. Claim Verdict & Confidence Calculation
           │
           ▼
  8. Overall Verdict Aggregation (TRUE / FALSE / UNCERTAIN / MIXED)
           │
           ▼
  9. Explainable Reasoning Generation (ExplanationService)
           │
           ▼
 10. Persistence (MongoDB Audit Trail)
```

---

## Key Features

- **Multi-Claim Extraction**: Automatically splits input stories into distinct atomic claims and evaluates each claim independently.
- **Real-Time Evidence Retrieval**: Queries web search endpoints via Serper API to retrieve organic snippets from news outlets, government sources, and fact-checking organization databases.
- **Sentence Transformer Semantic Matching**: Integrates a Python FastAPI service leveraging `sentence-transformers/all-MiniLM-L6-v2` cosine embeddings to compute semantic similarity between claims and evidence snippets.
- **Numerical Contradiction Prioritization**: Automatically extracts numbers, percentages, financial figures, and temporal units. Ensures that strong numerical contradictions (e.g. 75% GDP growth vs 7.8% actual growth) override semantic similarity.
- **Source Reliability Scoring**: Evaluates source domains (e.g. government `.gov`, Reuters, BBC, fact-checking orgs) to weight evidence reliability.
- **Verdict & Confidence Calculation**: Assigns standard verdicts (`TRUE`, `FALSE`, `UNCERTAIN`, `MIXED`) with mathematically calibrated confidence scores.
- **Explainable Reasoning**: Generates human-readable narrative explanations detailing why a specific verdict was reached and breaking down multi-claim outcomes.
- **Historical Audit Trail**: Persists verifications in MongoDB to allow viewing past claims, original text, claims breakdown, evidence sources, confidence, and explanations.

---

## Technology Stack

- **Backend**: Java 17+, Spring Boot 3 / 4, Spring Data MongoDB, Spring Web MVC, Jackson.
- **Database**: MongoDB (local or MongoDB Atlas).
- **NLP Service**: Python 3.10+, FastAPI, Uvicorn, PyTorch, `sentence-transformers` (`all-MiniLM-L6-v2`).
- **Frontend**: React 18, Vite, Vanilla CSS design system, Lucide React icons.
- **Evidence Search**: Serper Web Search API (`https://google.serper.dev/search`).

---

## Project Structure

```
TruthLens/
├── backend/                        # Spring Boot Backend
│   ├── src/main/java/truthlens_backend/
│   │   ├── config/                 # Global CorsConfig
│   │   ├── controller/             # Verification & Evidence REST Controllers
│   │   ├── dto/                    # VerificationRequest, ErrorResponse
│   │   ├── exception/              # GlobalExceptionHandler (@RestControllerAdvice)
│   │   ├── model/                  # Claim, Evidence, Verification MongoDB Documents
│   │   ├── repository/             # VerificationRepository
│   │   └── service/                # ClaimExtraction, EvidenceRetrieval, EvidenceAnalysis,
│   │                               # FactConsistency, SemanticSimilarity, Verdict, Explanation Services
│   ├── src/test/java/              # Unit & Integration Tests
│   ├── pom.xml                     # Maven dependencies
│   └── .env.example                # Backend environment template
├── frontend/                       # React + Vite Frontend
│   ├── src/                        # UI components & CSS design system
│   ├── package.json
│   └── vite.config.js
├── nlp-service/                    # Python FastAPI Transformer Service
│   ├── app.py                      # FastAPI app (/health, /similarity)
│   └── requirements.txt
├── .env.example                    # Root environment template
└── README.md                       # Comprehensive documentation
```

---

## Setup & Running Instructions

### Prerequisites
- **Java**: JDK 17 or higher
- **Node.js**: v18 or higher
- **Python**: 3.10 or higher
- **MongoDB**: Running locally on `mongodb://localhost:27017`

---

### 1. Start Python NLP Service

```powershell
cd nlp-service
python -m venv venv
.\venv\Scripts\activate      # On Linux/macOS: source venv/bin/activate
pip install -r requirements.txt
python app.py
```
*The NLP service runs on `http://127.0.0.1:8000`.*

---

### 2. Configure Environment & Start Backend

Copy `.env.example` to `.env` or set environment variables:

```powershell
cd backend
# Set environment variables (optional for live web search)
$env:SERPER_API_KEY="your_serper_key_here"
$env:MONGODB_URI="mongodb://localhost:27017/truthlens"

./mvnw spring-boot:run
```
*The backend runs on `http://localhost:8080`.*

---

### 3. Start Frontend

```powershell
cd frontend
npm install
npm run dev
```
*The frontend runs on `http://localhost:5173`.*

---

## Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SERPER_API_KEY` | Serper.dev Web Search API key | *(empty - evidence search returns empty if unconfigured)* |
| `MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/truthlens` |
| `PORT` | Backend server port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:5173,http://127.0.0.1:5173` |

---

## API Usage & Examples

### Create Verification (POST `/api/verifications`)

**Request:**
```bash
curl -X POST http://localhost:8080/api/verifications \
  -H "Content-Type: application/json" \
  -d '{
    "originalText": "India'\''s economy expanded by around 7.8% in 2025. India'\''s GDP growth was 75% in 2025."
  }'
```

**Response (Example Output):**
```json
{
  "id": "66e4a2b1f8...",
  "originalText": "India's economy expanded by around 7.8% in 2025. India's GDP growth was 75% in 2025.",
  "status": "MIXED",
  "confidence": 0.85,
  "createdAt": "2026-09-14T14:30:00",
  "explanation": "The submitted text contains claims with different verification outcomes. Claim 1 is supported by the available evidence, while Claim 2 is contradicted by the available evidence. Overall verdict: MIXED. 2 claims were analyzed separately: 1 supported, 1 contradicted, and 0 uncertain. Overall confidence is 85%.",
  "claims": [
    {
      "id": "claim-1-uuid",
      "text": "India's economy expanded by around 7.8% in 2025.",
      "verdict": "TRUE",
      "confidence": 0.90,
      "evidence": [...]
    },
    {
      "id": "claim-2-uuid",
      "text": "India's GDP growth was 75% in 2025.",
      "verdict": "FALSE",
      "confidence": 0.80,
      "evidence": [...]
    }
  ]
}
```

---

## Testing Instructions

### Run Backend Unit Tests
```powershell
cd backend
./mvnw test
```

### Run Frontend Production Build
```powershell
cd frontend
npm run build
```

---

## Limitations & Future Enhancements

- **Web Search Dependency**: Web retrieval requires a valid Serper API key; offline fallback relies on local numerical/fact consistency.
- **Multilingual Fact-Checking**: Current NLP model supports English claims; future iterations will expand to regional languages.
- **Publisher Bias Analysis**: Plans include integrating media credibility rating metrics from independent rating agencies.

---

## License
MIT License.
