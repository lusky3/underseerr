#!/bin/bash

# Script to configure Overseerr via API after initial setup

set -e

OVERSEERR_URL="http://localhost:5055"
API_KEY=""

echo "=========================================="
echo "Overseerr Configuration Script"
echo "=========================================="
echo ""

# Check if Overseerr is running
if ! curl -s "$OVERSEERR_URL/api/v1/status" > /dev/null; then
    echo "Error: Overseerr is not running at $OVERSEERR_URL"
    echo "Please start it first with: docker-compose up -d"
    exit 1
fi

echo "✓ Overseerr is running"
echo ""

# Check if already initialized
STATUS=$(curl -s "$OVERSEERR_URL/api/v1/status")
INITIALIZED=$(echo "$STATUS" | grep -o '"initialized":[^,}]*' | cut -d':' -f2)

if [ "$INITIALIZED" = "true" ]; then
    echo "✓ Overseerr is already initialized"
    echo ""
    echo "To get your API key:"
    echo "1. Open $OVERSEERR_URL"
    echo "2. Sign in"
    echo "3. Go to Settings → General"
    echo "4. Copy the API Key"
    echo ""
    echo "Then you can use the API:"
    echo "  curl -H \"X-Api-Key: YOUR_KEY\" $OVERSEERR_URL/api/v1/discover/trending"
else
    echo "⚠ Overseerr needs initial setup"
    echo ""
    echo "Please complete the setup wizard:"
    echo "1. Open $OVERSEERR_URL in your browser"
    echo "2. Follow the setup wizard"
    echo "3. Configure Plex/Radarr/Sonarr"
    echo "4. Run this script again"
fi

echo ""
echo "=========================================="
echo "Configuration Complete"
echo "=========================================="
