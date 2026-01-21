#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/../.env.ios"
# Explicitly sync first
./scripts/ios-sync.sh

# Run the command
ssh $IOS_SSH_OPTS "$IOS_USER@$IOS_HOST" "$@"
