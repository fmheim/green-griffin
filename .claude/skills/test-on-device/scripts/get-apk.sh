#!/usr/bin/env bash
#
# Provides a debug APK for the current checkout and prints its path on the
# last line of stdout.
#
#   get-apk.sh [auto|ci|build]
#
#   auto  (default) Use the CI artifact if HEAD is clean, pushed and has a
#         successful CI run with a non-expired APK; otherwise build locally.
#   ci    Only use the CI artifact; fail if there is none.
#   build Always build locally with ./gradlew assembleDebug.
#
# CI artifacts come from .github/workflows/ci.yml (artifact "green-griffin-*",
# kept 7 days) and are downloaded with the GitHub CLI into
# build/test-on-device/ci/<sha>/.

set -euo pipefail

mode="${1:-auto}"
case "$mode" in auto|ci|build) ;; *) echo "usage: $0 [auto|ci|build]" >&2; exit 2 ;; esac

root="$(git rev-parse --show-toplevel)"
cd "$root"
sha="$(git rev-parse HEAD)"

log() { echo "[get-apk] $*" >&2; }

try_ci() {
  if ! command -v gh >/dev/null 2>&1; then
    log "gh not installed, cannot download CI artifacts."
    return 1
  fi
  if [ -n "$(git status --porcelain)" ]; then
    log "Working tree has uncommitted changes; a CI APK would not contain them."
    return 1
  fi

  local run_id
  run_id="$(gh run list --workflow CI --commit "$sha" --status success --limit 1 \
    --json databaseId --jq '.[0].databaseId // empty' 2>/dev/null || true)"
  if [ -z "$run_id" ]; then
    local pending
    pending="$(gh run list --workflow CI --commit "$sha" --limit 1 \
      --json status,url --jq '.[0] | select(.status != "completed") | .url' 2>/dev/null || true)"
    if [ -n "$pending" ]; then
      log "CI for ${sha:0:7} is still running: $pending"
      log "Wait with: gh run watch $(basename "$pending") --exit-status"
    else
      log "No successful CI run for ${sha:0:7} (is it pushed and does a PR/develop build exist?)."
    fi
    return 1
  fi

  local name
  name="$(gh api "repos/{owner}/{repo}/actions/runs/$run_id/artifacts" \
    --jq '[.artifacts[] | select(.name | startswith("green-griffin-")) | select(.expired | not)][0].name // empty')"
  if [ -z "$name" ]; then
    log "CI run $run_id has no (non-expired) APK artifact."
    return 1
  fi

  local dir="$root/build/test-on-device/ci/$sha"
  if [ ! -f "$dir/$name.apk" ]; then
    log "Downloading $name from CI run $run_id ..."
    rm -rf "$dir"
    gh run download "$run_id" --name "$name" --dir "$dir" >&2
  else
    log "Using cached $name."
  fi
  log "Source: CI run $run_id (signed with the CI runner's debug key)."
  echo "$dir/$name.apk"
}

build_local() {
  if [ ! -f local.properties ]; then
    log "local.properties is missing (needs apiKey=...). Ask the user to create it; never print or commit it."
    return 1
  fi
  log "Building locally: ./gradlew assembleDebug ..."
  ./gradlew assembleDebug -q >&2
  log "Source: local build (signed with this machine's debug key)."
  echo "$root/app/build/outputs/apk/debug/app-debug.apk"
}

case "$mode" in
  ci) try_ci ;;
  build) build_local ;;
  auto) try_ci || build_local ;;
esac
