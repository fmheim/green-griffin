#!/usr/bin/env bash
#
# Green Griffin — setup script for Claude Code cloud environments.
#
# Paste the contents of this file into the "Setup script" field of the cloud
# environment (claude.ai/code -> environment settings). It is kept in the repo
# so the environment config is reviewable and versioned; the copy in the
# environment settings is the one that actually runs.
#
# Contract imposed by the platform (docs/claude-cloud-environment.md):
#   * runs as root on Ubuntu 24.04, before Claude Code launches
#   * must exit 0 — a non-zero exit makes the session fail to start
#   * must finish in roughly 5 minutes so the environment cache can build
#   * the filesystem is snapshotted afterwards and reused by later sessions,
#     so everything written outside the repo clone is installed only once
#
# Requires "Custom" network access including dl.google.com — the Trusted
# allowlist does not cover it, and both the SDK and Google's Maven repo live
# there. See docs/claude-cloud-environment.md.

set -uo pipefail   # no -e on purpose: this script must always exit 0

readonly SDK_ROOT="/opt/android-sdk"
readonly DEFAULT_COMPILE_SDK=37          # keep in sync with app/build.gradle.kts
readonly FALLBACK_CMDLINE_BUILD=15859902 # used if the version lookup fails
readonly GRADLE_WARMUP_TIMEOUT=150

log() { echo "[android-setup] $*"; }

# --- Android SDK ---------------------------------------------------------

install_cmdline_tools() {
  if [ -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    log "cmdline-tools already present, skipping download"
    return 0
  fi

  command -v unzip >/dev/null 2>&1 || apt-get install -y -qq unzip >/dev/null 2>&1

  # developer.android.com is on the default allowlist and always names the
  # current build, so the pin below is only a fallback.
  local zip
  zip=$(curl -fsS --max-time 30 https://developer.android.com/studio 2>/dev/null \
        | grep -o 'commandlinetools-linux-[0-9]*_latest\.zip' | sort -u | tail -1)
  if [ -z "$zip" ]; then
    zip="commandlinetools-linux-${FALLBACK_CMDLINE_BUILD}_latest.zip"
    log "version lookup failed, falling back to $zip"
  fi
  log "downloading $zip"

  local tmp="/tmp/cmdline-tools.zip"
  if ! curl -fsS --max-time 240 -o "$tmp" "https://dl.google.com/android/repository/$zip"; then
    log "ERROR: could not download the Android command-line tools."
    log "       Most likely dl.google.com is not in this environment's allowed"
    log "       domains. See docs/claude-cloud-environment.md."
    return 1
  fi

  # sdkmanager insists on living in cmdline-tools/<channel>/bin.
  rm -rf "$SDK_ROOT/cmdline-tools"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  unzip -q "$tmp" -d "$SDK_ROOT/cmdline-tools" && rm -f "$tmp"
  mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
}

# Highest stable build-tools the SDK repo offers, so this does not need
# bumping every time AGP moves its default forward.
latest_build_tools() {
  "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --list 2>/dev/null \
    | awk -F'|' '/^[[:space:]]+build-tools;/ { gsub(/[[:space:]]/, "", $1); print $1 }' \
    | grep -vE 'rc|alpha|beta' | sort -V | tail -1
}

# compileSdk from the checkout when it is already there, else the pin above.
compile_sdk() {
  local gradle_file
  for gradle_file in \
    "${CLAUDE_PROJECT_DIR:-}/app/build.gradle.kts" \
    /home/*/*/app/build.gradle.kts \
    /workspace/*/app/build.gradle.kts
  do
    [ -f "$gradle_file" ] || continue
    local found
    found=$(grep -oE 'compileSdk[[:space:]]*=[[:space:]]*[0-9]+' "$gradle_file" \
            | grep -oE '[0-9]+$' | head -1)
    if [ -n "$found" ]; then echo "$found"; return; fi
  done
  echo "$DEFAULT_COMPILE_SDK"
}

install_packages() {
  local sdkmanager="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  local sdk build_tools
  sdk=$(compile_sdk)
  build_tools=$(latest_build_tools)
  [ -n "$build_tools" ] || build_tools="build-tools;${sdk}.0.0"

  log "accepting licenses"
  yes 2>/dev/null | "$sdkmanager" --licenses >/dev/null 2>&1

  log "installing platform-tools, platforms;android-${sdk}, ${build_tools}"
  "$sdkmanager" --install \
    "platform-tools" "platforms;android-${sdk}" "$build_tools" >/dev/null
}

# --- Environment ---------------------------------------------------------

# The snapshot keeps files, not exported variables, so persist them in a
# profile script. The SessionStart hook re-exports them per session too.
write_profile() {
  cat > /etc/profile.d/android-sdk.sh <<'PROFILE'
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools"
PROFILE
  chmod 644 /etc/profile.d/android-sdk.sh
}

# --- Gradle warm-up ------------------------------------------------------

# Downloads the Gradle distribution and the build's plugins into
# /root/.gradle, which the snapshot keeps. Strictly best-effort: the first
# real build in a session still fetches the app's own dependencies.
warm_gradle() {
  local project_dir=""
  local candidate
  for candidate in \
    "${CLAUDE_PROJECT_DIR:-}" \
    /home/*/*/gradlew \
    /workspace/*/gradlew
  do
    [ -n "$candidate" ] || continue
    if [ -x "${candidate%/gradlew}/gradlew" ]; then project_dir="${candidate%/gradlew}"; break; fi
  done
  if [ -z "$project_dir" ]; then
    log "no checkout found, skipping Gradle warm-up"
    return 0
  fi

  # The secrets Gradle plugin needs an apiKey to exist for configuration to
  # succeed. A placeholder is enough to build; the hook writes the real file.
  if [ ! -f "$project_dir/local.properties" ]; then
    printf 'sdk.dir=%s\napiKey=placeholder\n' "$SDK_ROOT" > "$project_dir/local.properties"
  fi

  log "warming Gradle caches (up to ${GRADLE_WARMUP_TIMEOUT}s)"
  ( cd "$project_dir" \
    && ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT" \
       timeout "$GRADLE_WARMUP_TIMEOUT" ./gradlew --no-daemon --quiet help \
       >/dev/null 2>&1 ) \
    || log "Gradle warm-up did not finish — not fatal, builds still work"
}

# --- Main ----------------------------------------------------------------

main() {
  if install_cmdline_tools && install_packages; then
    write_profile
    log "Android SDK ready at $SDK_ROOT"
    warm_gradle
  else
    log "WARNING: Android SDK setup incomplete — Gradle builds will fail."
    log "         Check the environment's allowed domains, then restart a session."
  fi
  log "done"
}

main
exit 0
