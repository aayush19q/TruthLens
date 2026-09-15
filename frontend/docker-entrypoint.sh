#!/bin/sh
# ============================================================
# TruthLens Frontend — Dynamic nginx config entrypoint
# ============================================================
#
# When BACKEND_URL is set (Docker Compose), nginx proxies
# /api/* requests to the backend container.
#
# When BACKEND_URL is unset (Render), no proxy block is
# generated — the React app uses VITE_API_BASE_URL directly.
# ============================================================

set -e

CONF="/etc/nginx/conf.d/default.conf"

# ---------- Build the API proxy block (or leave it empty) ----
if [ -n "$BACKEND_URL" ]; then
  API_PROXY_BLOCK="
    # Proxy API requests to the backend service
    location /api/ {
        proxy_pass ${BACKEND_URL}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
"
else
  API_PROXY_BLOCK=""
fi

# ---------- Write the final nginx config ---------------------
cat > "$CONF" <<NGINX_EOF
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript image/svg+xml;
    gzip_min_length 256;
${API_PROXY_BLOCK}
    # Serve static assets with caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        try_files \$uri =404;
    }

    # SPA fallback — serve index.html for all non-file routes
    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX_EOF

echo "==> nginx config generated (BACKEND_URL=${BACKEND_URL:-<unset>})"

# ---------- Start nginx --------------------------------------
exec nginx -g "daemon off;"
