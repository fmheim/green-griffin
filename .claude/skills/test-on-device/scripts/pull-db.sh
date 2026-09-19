#!/usr/bin/env bash
#
# Copies the app's Room database (user_data.db plus its -wal/-shm files) from
# the device and prints the local path of user_data.db on the last line.
#
#   pull-db.sh [out_dir]
#
# Works only for debuggable builds (uses run-as). Target device: $ANDROID_SERIAL
# if set, otherwise the only connected device. Default out_dir is
# build/test-on-device/db/<timestamp>/ so earlier snapshots stay comparable.
#
# The WAL file holds recent writes that are not yet in user_data.db. Keep the
# three files together; SQLite (e.g. query-db.py) replays the WAL on open.

set -euo pipefail

pkg="com.felix.greengriffin"
root="$(git rev-parse --show-toplevel)"
out_dir="${1:-$root/build/test-on-device/db/$(date +%Y%m%d-%H%M%S)}"

if ! command -v adb >/dev/null 2>&1; then
  sdk="$(android info sdk 2>/dev/null || true)"
  command -v cygpath >/dev/null 2>&1 && [ -n "$sdk" ] && sdk="$(cygpath "$sdk")"
  PATH="$PATH:${sdk:-${ANDROID_HOME:-}}/platform-tools"
fi

mkdir -p "$out_dir"
for f in user_data.db user_data.db-wal user_data.db-shm; do
  if adb shell run-as "$pkg" test -f "databases/$f"; then
    adb exec-out run-as "$pkg" cat "databases/$f" > "$out_dir/$f"
  fi
done

if [ ! -s "$out_dir/user_data.db" ]; then
  echo "[pull-db] Could not read databases/user_data.db (app not installed, never launched, or not debuggable)." >&2
  exit 1
fi
echo "[pull-db] Pulled $(ls "$out_dir" | tr '\n' ' ')" >&2
echo "$out_dir/user_data.db"
