#!/bin/bash
# Helper script to run docker compose with .env.dev file
# Usage: ./docker-compose-dev.sh [docker compose commands]
# Example: ./docker-compose-dev.sh up
#          ./docker-compose-dev.sh build
#          ./docker-compose-dev.sh down

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if .env.dev exists
if [ ! -f ".env.dev" ]; then
    echo "Error: .env.dev file not found in $SCRIPT_DIR"
    echo "Please create .env.dev file with your environment variables"
    exit 1
fi

# Run docker compose with --env-file .env.dev and pass all arguments
docker compose --env-file .env.dev -f compose.dev.yml "$@"

