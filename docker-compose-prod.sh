#!/bin/bash
# Helper script to run docker compose with .env file for production
# Usage: ./docker-compose-prod.sh [docker compose commands]
# Example: ./docker-compose-prod.sh up -d
#          ./docker-compose-prod.sh build
#          ./docker-compose-prod.sh down

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "Error: .env file not found in $SCRIPT_DIR"
    echo "Please create .env file with your production environment variables"
    echo "You can copy .env.template to .env and fill in the values:"
    echo "  cp .env.template .env"
    echo "  # Edit .env with your production values"
    exit 1
fi

# Run docker compose with --env-file .env and pass all arguments
docker compose --env-file .env -f compose.prod.yml "$@"

