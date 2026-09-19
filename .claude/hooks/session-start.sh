#!/usr/bin/env bash
#
# SessionStart hook — per-session wiring for Claude Code cloud sessions.
#
# The environment setup script (.claude/scripts/cloud-setup.sh) installs the
# Android SDK once and the platform snapshots it. This hook does the part that
# cannot be snapshotted: exporting variables into the session and writing
# local.properties, which lives in the repo clone and is gitignored, so every
# session starts without it.
#
# Runs on cloud sessions only, so local checkouts and Android Studio are
# untouched.

set -uo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0

log() { echo "[session-start] $*"; }

project_dir="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

# --- Locate the SDK ------------------------------------------------------

sdk_dir=""
for candidate in \
  "${ANDROID_HOME:-}" \
  "${ANDROID_SDK_ROOT:-}" \
  /opt/android-sdk \
  "$HOME/Android/Sdk"
do
  if [ -n "$candidate" ] && [ -d "$candidate/platforms" ]; then
    sdk_dir="$candidate"
    break
  fi
done

if [ -z "$sdk_dir" ]; then
  log "No Android SDK found. Gradle tasks that need it (assembleDebug,"
  log "testDebugUnitTest, lintDebug) will fail until the environment's setup"
  log "script has run. Fix: set the environment's Setup script to the contents"
  log "of .claude/scripts/cloud-setup.sh and its network access to Custom with"
  log "dl.google.com allowed, then start a new session."
  log "Details: docs/claude-cloud-environment.md"
  exit 0
fi

# --- Export for the session ---------------------------------------------

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export ANDROID_HOME=\"$sdk_dir\""
    echo "export ANDROID_SDK_ROOT=\"$sdk_dir\""
    echo "export PATH=\"\$PATH:$sdk_dir/cmdline-tools/latest/bin:$sdk_dir/platform-tools\""
  } >> "$CLAUDE_ENV_FILE"
fi

# --- local.properties ----------------------------------------------------

# The secrets Gradle plugin reads apiKey from here, so Gradle cannot even
# configure the project without it. A placeholder builds fine; only the
# Gemini code path needs a real key, and the app does not call it yet.
# Never printed: keep it out of the log.
props="$project_dir/local.properties"
api_key="${GREEN_GRIFFIN_API_KEY:-}"

if [ -z "$api_key" ] && [ -f "$props" ]; then
  api_key=$(sed -n 's/^apiKey=\(.*\)$/\1/p' "$props" | head -1)
fi
[ -n "$api_key" ] || api_key="placeholder"

umask 077
printf 'sdk.dir=%s\napiKey=%s\n' "$sdk_dir" "$api_key" > "$props"

if [ "$api_key" = "placeholder" ]; then
  log "local.properties written with sdk.dir and a placeholder apiKey"
else
  log "local.properties written with sdk.dir and an apiKey from the environment"
fi

# --- Report --------------------------------------------------------------

platforms=$(ls "$sdk_dir/platforms" 2>/dev/null | tr '\n' ' ')
build_tools=$(ls "$sdk_dir/build-tools" 2>/dev/null | tr '\n' ' ')
log "ANDROID_HOME=$sdk_dir"
log "platforms: ${platforms:-none}"
log "build-tools: ${build_tools:-none}"

if [ ! -e /dev/kvm ]; then
  log "No /dev/kvm: no emulator, so connectedDebugAndroidTest cannot run here."
  log "Use ./gradlew testDebugUnitTest and lintDebug instead."
fi

exit 0
