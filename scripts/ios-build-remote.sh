#!/usr/bin/env bash
set -euo pipefail
export PATH="/opt/homebrew/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/iosApp"

echo "Generating Xcode project..."
xcodegen generate

echo "Building iOS App..."
xcodebuild \
  -project iOSApp.xcodeproj \
  -scheme iOSApp \
  -destination 'platform=iOS Simulator,name=iPhone 14,OS=latest' \
  -derivedDataPath build \
  build
