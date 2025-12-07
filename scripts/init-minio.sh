#!/bin/bash
# MinIO initialization script
# Configures CORS and bucket policies for browser access

set -e

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
until curl -f http://localhost:9000/minio/health/live 2>/dev/null; do
  sleep 2
done

echo "MinIO is ready. Configuring CORS and bucket policies..."

# Install mc (MinIO Client) if not available
if ! command -v mc &> /dev/null; then
  echo "Installing MinIO Client..."
  # Download mc for your platform (adjust as needed)
  # For macOS/Linux:
  wget -q https://dl.min.io/client/mc/release/linux-amd64/mc -O /tmp/mc
  chmod +x /tmp/mc
  MC=/tmp/mc
else
  MC=mc
fi

# Configure MinIO alias
$MC alias set local http://localhost:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD}

# Configure CORS for the bucket
echo "Configuring CORS..."
$MC anonymous set public local/${AWS_S3_BUCKET} || true

# Set CORS rules (allow all origins for development)
cat > /tmp/cors.json <<EOF
{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
}
EOF

$MC anonymous set-json /tmp/cors.json local/${AWS_S3_BUCKET} || echo "CORS configuration may need to be set via MinIO console"

echo "MinIO initialization complete!"

