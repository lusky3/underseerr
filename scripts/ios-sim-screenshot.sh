#!/usr/bin/env bash
set -e

# Capture screenshot to a temp file
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="sim_screenshot_$TIMESTAMP.png"
FILEPATH="/tmp/$FILENAME"

xcrun simctl io booted screenshot "$FILEPATH" > /dev/null 2>&1

# Return only the filepath so it can be captured by the caller
echo "$FILEPATH"
