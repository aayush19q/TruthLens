# ============================================================
# TruthLens Python NLP Service Dockerfile
# ============================================================
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for torch
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user with a writable home directory (before model download)
RUN addgroup --system nlp && adduser --system --ingroup nlp --home /home/nlp nlp

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

# Pre-download the Sentence Transformer model into the nlp user's cache
# so it is accessible at runtime without root privileges
ENV HF_HOME=/home/nlp/.cache/huggingface
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')" && \
    chown -R nlp:nlp /home/nlp/.cache

USER nlp

ENV HOST=0.0.0.0
ENV PORT=8000
EXPOSE 8000

# Use wget -qO- (GET request) instead of --spider (HEAD) because FastAPI
# only defines GET on /health and returns 405 for HEAD
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8000/health > /dev/null 2>&1 || exit 1

CMD ["python", "app.py"]

