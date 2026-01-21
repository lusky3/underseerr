#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/../.env.ios"

REMOTE=$(ssh $IOS_SSH_OPTS "$IOS_USER@$IOS_HOST" "bash $IOS_REMOTE_ROOT/scripts/ios-sim-screenshot.sh" | awk '{print $NF}')

scp "$IOS_USER@$IOS_HOST:$REMOTE" ./ios-screenshot.png

echo "Saved screenshot to ./ios-screenshot.png"
