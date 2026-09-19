---
name: test-on-device
description: Install Green Griffin on a connected Android device or emulator and test, verify or debug it there. Gets the debug APK from the GitHub Actions CI build of the current commit when one exists, otherwise builds it locally. Then drives the app with the android CLI (layout, screen capture) and adb input, and inspects logcat, the pulled Room database (saved games, level progress) and dumpsys. Supports an iterative loop of adding temporary logs, fixing, rebuilding and re-verifying. Use when asked to test on a device or phone, verify a change on a real device, reproduce or debug a bug in the running app, check a crash, inspect saved game state, or "try it on my phone". Pass extra instructions to say what to test; without them it tests the current changes.
argument-hint: "[what to test or debug, e.g. 'placing a joker in FreePlay saves correctly']"
---

# Test on device

Verify or debug Green Griffin (`com.felix.greengriffin`, launcher activity `.MainActivity`) on a real device or emulator. The loop is: **get APK → install → exercise → observe → (add logs / fix → rebuild) → report.**

This skill builds on the **android-cli** skill. Load it first. Before the first `android layout` or `android screen` call, read `.claude/skills/android-cli/references/interact.md` and follow it: check the layout before a screenshot, look at every screenshot yourself, and check that a field is focused before typing into it.

Paths below are relative to the repo root. Scripts are in `.claude/skills/test-on-device/scripts/`. Put all outputs (screenshots, logcat dumps, DB snapshots) in `build/test-on-device/`. That folder is gitignored.

## 0. Setup and device

- Shell state does not carry over between tool calls. In Git Bash, start each device command with the following line if `adb` is not on PATH (it is not on PATH on the Windows dev machine):
  ```bash
  export PATH="$PATH:$(cygpath "$(android info sdk)")/platform-tools"
  ```
  On Linux, use `$(android info sdk)/platform-tools` without `cygpath`. If `android` itself is missing, install it as the android-cli skill describes.
- Run `adb devices -l`.
  - **No device:** list emulators with `android emulator list` and offer to start one with `android emulator start <name>`, or ask the user to connect a phone with USB debugging on. Claude Code cloud sessions have no device. Say so and stop; don't pretend to test.
  - **More than one device:** ask which one, then prefix commands with `ANDROID_SERIAL=<serial>` or pass `-s <serial>` / `--device=<serial>`.
- The device may be the user's personal phone with real saved games. Treat its data as valuable. Before any step that wipes it (uninstall, `pm clear`, a DB version bump), back up the database and ask the user first.

## 1. Decide what to test

1. If the user gave instructions (skill arguments or the conversation), they define the scope.
2. Otherwise, work out the scope from the changes:
   ```bash
   git status --short
   git diff origin/develop...HEAD --stat && git diff --stat
   ```
   Read the diffs and map changed files to the affected screens and behaviour. `board/presentation` covers the board, stones, jokers and saving (FreePlay and Trails). `trails/` covers the level list and progress. `board/data` covers persistence, which you verify through the DB. `core/presentation` covers theme and drawing.
3. Write a short test plan before touching the device. Give each step an expected result and the signal that proves it: UI (layout or screenshot), logcat, or a DB row. Include at least one regression check near the change, e.g. that the app restores the saved game after a restart. For a bug, the first step is to reproduce it reliably.
   If the user wants a formal pass/fail run, write the plan as an XML journey and evaluate it as `.claude/skills/android-cli/references/journeys.md` describes.

## 2. Get the APK

```bash
bash .claude/skills/test-on-device/scripts/get-apk.sh        # auto (default)
bash .claude/skills/test-on-device/scripts/get-apk.sh build  # force a local build
bash .claude/skills/test-on-device/scripts/get-apk.sh ci     # CI artifact only
```
The APK path is printed on the last line of stdout. Diagnostics go to stderr.

- `auto` uses the CI APK when the working tree is clean and CI (`.github/workflows/ci.yml`) has built `HEAD` successfully. That happens for PR and `develop` builds, and artifacts are kept for 7 days. Otherwise it runs `./gradlew assembleDebug`. If CI is still running, the script prints the `gh run watch` command. Wait for CI if the build is almost done, otherwise build locally.
- Once you start changing code (logs or fixes), the tree is dirty, so the script always builds locally. That is expected.
- **Signing:** CI APKs are signed with the CI runner's debug key, and local builds with this machine's key. Switching between them on one device fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (see step 3). If you expect to iterate, start with a local build (`build`) so you don't have to switch keys partway through.

## 3. Install and launch

```bash
android run --apks=<apk> --device=<serial>      # install + launch
# fallback: adb install -r -t <apk> && adb shell am start -n com.felix.greengriffin/.MainActivity
```
Apps installed from Android Studio are marked test-only, so plain `adb install` needs `-t`.

**`INSTALL_FAILED_UPDATE_INCOMPATIBLE`** (signature mismatch): don't uninstall without asking.
1. Back up the database: `bash .claude/skills/test-on-device/scripts/pull-db.sh build/test-on-device/db/backup-before-uninstall`.
2. Ask the user whether to uninstall. Uninstalling wipes saved games and progress.
3. `adb uninstall com.felix.greengriffin`, install, then launch the app once so Room creates the database.
4. If the user wants the data back, restore the backup as shown in [references/debugging.md](references/debugging.md#restore-a-database-backup). Restore it only if the Room `version` in `UserDatabase` did not change.

**Room version bump:** if the diff changes `version` in `board/data/local/UserDatabase.kt` without a `Migration`, the app crashes on first launch (there is deliberately no destructive fallback on the user database). Back up the DB before installing, and tell the user this will happen.

## 4. Exercise the app

Before each scenario, get a clean baseline:
```bash
adb logcat -c                      # clear old logs
adb shell am start -n com.felix.greengriffin/.MainActivity
adb shell pidof -s com.felix.greengriffin   # note the PID
```
Navigate with `android layout` (use `--diff` after actions to keep output small), `android screen capture [--annotate] -o build/test-on-device/<name>.png` and `adb shell input tap|swipe|text|keyevent`.

Notes for this app:
- **Navigation:** Home → FreePlay (board screen) or Trails → level list → level (board screen). Back is `adb shell input keyevent 4`.
- **Dragging stones:** stones use the platform drag-and-drop (`dragAndDropSource`, the drag starts on press). Use `adb shell input draganddrop <x1> <y1> <x2> <y2> 1500`. If the drop doesn't register, try `adb shell input swipe <x1> <y1> <x2> <y2> 1500`, or `adb shell input motionevent DOWN|MOVE|UP x y` with short sleeps in between. Check the result with `layout --diff` or a screenshot.
- The board is drawn in Compose, and letters on the board may be missing from `layout`. Use a screenshot (`--annotate` for tap targets) whenever the layout doesn't show what you need.
- Also test the things that often break around a change: process death and restore, rotation or config changes, and restarting to check persistence. [references/debugging.md](references/debugging.md) has the commands.

## 5. Observe

Check the relevant signals after every meaningful step, not only at the end.

- **Crashes and errors:** `adb logcat -d -b crash`, and `adb logcat -d --pid=<pid> '*:W'` for app warnings and errors. If `pidof` returns nothing after an action, the app died. Read the crash buffer.
- **App logs:** `adb logcat -d --pid=<pid>`, or filter by tag: `adb logcat -d -s GGDBG:V AndroidRuntime:E`.
- **Database:** Room keeps saved games (`game_state`, one JSON blob per game mode/level) and Trails progress (`completed_levels`) in `user_data.db`; the word list lives separately in `dictionary_cache.db`.
  ```bash
  db=$(bash .claude/skills/test-on-device/scripts/pull-db.sh | tail -1)
  py .claude/skills/test-on-device/scripts/query-db.py "$db"            # overview
  py .claude/skills/test-on-device/scripts/query-db.py "$db" \
    "SELECT level, json_extract(game_state_json,'$.totalPoints') FROM game_state WHERE game_mode_id=2"
  ```
  (Use `python3` instead of `py` on Linux.) Each pull goes into a new timestamped folder, so you can compare snapshots from before and after an action. The app saves asynchronously: wait a moment, or background the app (`keyevent 3`), before pulling. `game_mode_id` follows `GameMode.FREE_PLAY_ID = 1` (with `level` -1) and `TRAILS_ID = 2` (with `level` = level index). The JSON in `game_state_json` is the serialized `GameState` (`stonesInHand`, `totalPoints`, …).
- **Everything else** (dumpsys, performance, SQL statement logging, screen recording, settings toggles): see [references/debugging.md](references/debugging.md).

## 6. Debug and fix loop

When something is wrong, or you cannot yet explain what you see:

1. **Form a hypothesis** from the code and the signals you have.
2. **Add temporary logs** if you need more signal. Use the single tag `GGDBG` so the logs are easy to filter and to remove:
   ```kotlin
   android.util.Log.d("GGDBG", "onEvent $event -> stones=${state.stonesInHand.size}") // GGDBG: remove
   ```
   Log at decision points: `onEvent` entry, use case results (`PlacementValidation`/`WordValidation`), repository reads and writes, `LaunchedEffect` triggers. Don't log `BuildConfig.apiKey` or anything derived from it.
3. **Rebuild and reinstall:** `get-apk.sh build`, then `android run --apks=...`. Delta install keeps it fast. Repeat the exact reproduction steps, then filter logcat for `GGDBG`.
4. **Fix** the root cause, following the conventions in `CLAUDE.md`. Rerun the original scenario *and* the regression checks. If the logic is pure domain logic, also run `./gradlew testDebugUnitTest`, and consider adding a unit test that pins the bug down.
5. Iterate until the plan passes. If you are stuck after a few rounds, stop and report what you know rather than guessing.

## 7. Clean up and report

- Remove every temporary log. `git grep -n GGDBG` must return nothing, unless the user asked to keep some logs, in which case they should be proper logs without the tag. Rebuild once afterwards if you changed code.
- Reset any device settings you changed (rotation, dark mode, animation scales, font scale), and delete files you pushed to the device.
- Don't commit unless asked.
- Report briefly:
  - which APK you used (CI run or local build, and the commit)
  - the device (model and API level)
  - what you tested, and pass/fail per step, with evidence (screenshot paths, key log lines, DB rows)
  - bugs you found and fixes you made (files)
  - anything you could not verify
