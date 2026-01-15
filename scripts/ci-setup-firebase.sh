#!/bin/bash
# Reconstruct google-services.json from environment variable
# Usage: ./scripts/ci-setup-firebase.sh
# Requires: FIREBASE_GOOGLE_SERVICES_JSON environment variable containing the file contents

set -e

# Use specific variables if available, otherwise fall back to generic for release
DEBUG_JSON="${FIREBASE_GOOGLE_SERVICES_JSON_DEBUG}"
RELEASE_JSON="${FIREBASE_GOOGLE_SERVICES_JSON_RELEASE:-$FIREBASE_GOOGLE_SERVICES_JSON}"

if [ -n "$DEBUG_JSON" ]; then
    echo "Reconstructing androidApp/src/debug/google-services.json..."
    mkdir -p androidApp/src/debug
    echo "$DEBUG_JSON" > androidApp/src/debug/google-services.json
    echo "Debug config reconstructed."
else
    echo "Warning: FIREBASE_GOOGLE_SERVICES_JSON_DEBUG not set. Debug build might fail."
fi

if [ -n "$RELEASE_JSON" ]; then
    echo "Reconstructing androidApp/src/release/google-services.json..."
    mkdir -p androidApp/src/release
    echo "$RELEASE_JSON" > androidApp/src/release/google-services.json
    echo "Release config reconstructed."
else
    echo "Warning: FIREBASE_GOOGLE_SERVICES_JSON_RELEASE (or FIREBASE_GOOGLE_SERVICES_JSON) not set."
fi

