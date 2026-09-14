from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import torch


# ============================================================
# APPLICATION
# ============================================================

app = FastAPI(
    title="TruthLens NLP Service",
    version="1.0.0"
)


# ============================================================
# LOAD TRANSFORMER MODEL
# ============================================================

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"

print("Loading Sentence Transformer model...")

model = SentenceTransformer(
    MODEL_NAME
)

print("Sentence Transformer model loaded successfully.")


# ============================================================
# REQUEST MODEL
# ============================================================

class SimilarityRequest(BaseModel):
    text1: str
    text2: str


# ============================================================
# RESPONSE MODEL
# ============================================================

class SimilarityResponse(BaseModel):
    similarity: float
    model: str


# ============================================================
# HEALTH CHECK
# ============================================================

@app.get("/health")
def health():
    return {
        "status": "UP",
        "model": MODEL_NAME
    }


# ============================================================
# SEMANTIC SIMILARITY
# ============================================================

@app.post(
    "/similarity",
    response_model=SimilarityResponse
)
def similarity(request: SimilarityRequest):
    try:
        text1 = request.text1.strip() if request.text1 else ""
        text2 = request.text2.strip() if request.text2 else ""

        if not text1 or not text2:
            return SimilarityResponse(
                similarity=0.0,
                model=MODEL_NAME
            )

        # Generate normalized embeddings
        embeddings = model.encode(
            [text1, text2],
            convert_to_tensor=True,
            normalize_embeddings=True
        )

        # Cosine similarity via dot product of normalized embeddings
        score = torch.dot(
            embeddings[0],
            embeddings[1]
        ).item()

        score = max(0.0, min(1.0, float(score)))

        return SimilarityResponse(
            similarity=round(score, 4),
            model=MODEL_NAME
        )
    except Exception as e:
        print(f"Error computing similarity: {e}")
        return SimilarityResponse(
            similarity=0.0,
            model=MODEL_NAME
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=8000
    )