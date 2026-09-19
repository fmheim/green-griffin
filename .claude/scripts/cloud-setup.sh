#!/bin/bash
#
# Green Griffin - setup script for Claude Code cloud environments.
#
# Paste the contents of this file into the "Setup script" field of the cloud
# environment (claude.ai/code -> environment settings). The copy in the
# environment settings is the one that runs; this file is kept in the repo so
# the configuration is reviewable and versioned.
#
# Platform contract (see docs/claude-cloud-environment.md):
#   * runs as root on Ubuntu 24.04, before Claude Code launches
#   * must exit 0 - a non-zero exit makes the session fail to start
#   * must finish in roughly 5 minutes so the environment cache can build
#   * the filesystem is snapshotted afterwards, so this runs once per cache
#
# Requires "Custom" network access including dl.google.com - the Trusted
# allowlist does not cover it, and both the SDK and Google's Maven repo live
# there.
#
# Deliberately written flat: no shell functions, no "local", and no bashisms
# beyond the basics. The platform copies this into a generated wrapper script
# before running it, and a function-based version failed there with
# "local: can only be used in a function".

set -u

SDK_ROOT=/opt/android-sdk
DEFAULT_COMPILE_SDK=37          # keep in sync with app/build.gradle.kts
FALLBACK_CMDLINE_BUILD=15859902 # used only if the version lookup fails
GRADLE_WARMUP_TIMEOUT=150
SDK_READY=no

# ---------------------------------------------------------------- SDK tools

if [ -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "[android-setup] cmdline-tools already present, skipping download"
  SDK_READY=yes
else
  command -v unzip >/dev/null 2>&1 || apt-get install -y -qq unzip >/dev/null 2>&1

  # developer.android.com is on the default allowlist and always names the
  # current build, so the pin above is only a fallback.
  CMDLINE_ZIP=$(curl -fsS --max-time 30 https://developer.android.com/studio 2>/dev/null \
    | grep -o 'commandlinetools-linux-[0-9]*_latest\.zip' | sort -u | tail -1)
  if [ -z "$CMDLINE_ZIP" ]; then
    CMDLINE_ZIP="commandlinetools-linux-${FALLBACK_CMDLINE_BUILD}_latest.zip"
    echo "[android-setup] version lookup failed, falling back to $CMDLINE_ZIP"
  fi

  echo "[android-setup] downloading $CMDLINE_ZIP"
  if curl -fsS --max-time 240 -o /tmp/cmdline-tools.zip \
      "https://dl.google.com/android/repository/$CMDLINE_ZIP"; then
    # sdkmanager insists on living in cmdline-tools/<channel>/bin.
    rm -rf "$SDK_ROOT/cmdline-tools"
    mkdir -p "$SDK_ROOT/cmdline-tools"
    if unzip -q /tmp/cmdline-tools.zip -d "$SDK_ROOT/cmdline-tools"; then
      rm -f /tmp/cmdline-tools.zip
      mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest" \
        && SDK_READY=yes
    fi
  else
    echo "[android-setup] ERROR: could not download the Android command-line tools."
    echo "[android-setup]        Most likely dl.google.com is not in this environment's"
    echo "[android-setup]        allowed domains. See docs/claude-cloud-environment.md."
  fi
fi

# ------------------------------------------------------------- SDK packages

if [ "$SDK_READY" = yes ]; then
  SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

  # compileSdk from the checkout when it is already there, else the pin above.
  COMPILE_SDK=
  for GRADLE_FILE in \
    "${CLAUDE_PROJECT_DIR:-}/app/build.gradle.kts" \
    /home/*/*/app/build.gradle.kts \
    /workspace/*/app/build.gradle.kts
  do
    [ -f "$GRADLE_FILE" ] || continue
    COMPILE_SDK=$(grep -oE 'compileSdk[[:space:]]*=[[:space:]]*[0-9]+' "$GRADLE_FILE" \
      | grep -oE '[0-9]+$' | head -1)
    [ -z "$COMPILE_SDK" ] || break
  done
  [ -n "$COMPILE_SDK" ] || COMPILE_SDK=$DEFAULT_COMPILE_SDK

  # Newest stable build-tools, so this does not need bumping every time AGP
  # moves its default forward.
  BUILD_TOOLS=$("$SDKMANAGER" --list 2>/dev/null \
    | awk -F'|' '/^[[:space:]]+build-tools;/ { gsub(/[[:space:]]/, "", $1); print $1 }' \
    | grep -vE 'rc|alpha|beta' | sort -V | tail -1)
  [ -n "$BUILD_TOOLS" ] || BUILD_TOOLS="build-tools;${COMPILE_SDK}.0.0"

  echo "[android-setup] accepting licences"
  yes 2>/dev/null | "$SDKMANAGER" --licenses >/dev/null 2>&1

  echo "[android-setup] installing platform-tools, platforms;android-${COMPILE_SDK}, ${BUILD_TOOLS}"
  if "$SDKMANAGER" --install \
      "platform-tools" "platforms;android-${COMPILE_SDK}" "$BUILD_TOOLS" >/dev/null; then
    echo "[android-setup] Android SDK ready at $SDK_ROOT"
  else
    echo "[android-setup] ERROR: sdkmanager could not install the SDK packages."
    SDK_READY=no
  fi
fi

# --------------------------------------------------------------- Environment

# The snapshot keeps files, not exported variables, so persist them in a
# profile script. The SessionStart hook re-exports them per session too.
if [ "$SDK_READY" = yes ]; then
  cat > /etc/profile.d/android-sdk.sh <<'PROFILE'
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools"
PROFILE
  chmod 644 /etc/profile.d/android-sdk.sh
fi

# -------------------------------------------------------------- Gradle warm-up

# Downloads the Gradle distribution and the build's plugins into /root/.gradle,
# which the snapshot keeps. Strictly best-effort: the first real build in a
# session still fetches the app's own dependencies.
if [ "$SDK_READY" = yes ]; then
  PROJECT_DIR=
  for CANDIDATE in "${CLAUDE_PROJECT_DIR:-}" /home/*/*/gradlew /workspace/*/gradlew; do
    [ -n "$CANDIDATE" ] || continue
    CANDIDATE_DIR=${CANDIDATE%/gradlew}
    if [ -x "$CANDIDATE_DIR/gradlew" ]; then
      PROJECT_DIR=$CANDIDATE_DIR
      break
    fi
  done

  if [ -z "$PROJECT_DIR" ]; then
    echo "[android-setup] no checkout found, skipping Gradle warm-up"
  else
    # The secrets Gradle plugin needs an apiKey to exist for configuration to
    # succeed. A placeholder builds fine; the hook writes the real file.
    if [ ! -f "$PROJECT_DIR/local.properties" ]; then
      printf 'sdk.dir=%s\napiKey=placeholder\n' "$SDK_ROOT" > "$PROJECT_DIR/local.properties"
    fi
    echo "[android-setup] warming Gradle caches (up to ${GRADLE_WARMUP_TIMEOUT}s)"
    cd "$PROJECT_DIR" || exit 0
    ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT" \
      timeout "$GRADLE_WARMUP_TIMEOUT" ./gradlew --no-daemon --quiet help >/dev/null 2>&1 \
      || echo "[android-setup] Gradle warm-up did not finish - not fatal, builds still work"
  fi
fi

if [ "$SDK_READY" != yes ]; then
  echo "[android-setup] WARNING: Android SDK setup incomplete - Gradle builds will fail."
  echo "[android-setup]          Check the environment's allowed domains, then start a new session."
fi

echo "[android-setup] done"
exit 0
