# On-device debugging reference

A collection of adb commands for testing `com.felix.greengriffin`. All of them assume `adb` is on PATH (see SKILL.md step 0) and that a single device is connected. With more than one device, add `-s <serial>`.

## Logcat

| Goal | Command |
|---|---|
| Clear before reproducing | `adb logcat -c` |
| Dump app logs only | `adb logcat -d -v time --pid=$(adb shell pidof -s com.felix.greengriffin)` |
| Warnings and errors of the app | `adb logcat -d --pid=<pid> '*:W'` |
| Crash stack traces (survive the process dying) | `adb logcat -d -b crash` |
| Temporary debug logs only | `adb logcat -d -s GGDBG:V` |
| Follow live for N seconds | `timeout 15 adb logcat --pid=<pid> > build/test-on-device/live.log` |
| Save a full dump as evidence | `adb logcat -d -v threadtime > build/test-on-device/logcat-<step>.txt` |

- `--pid` stops matching once the process restarts, so fetch the PID again after a crash or a relaunch.
- For a crash that happens during startup, use `adb logcat -d -b crash,main -v time | grep -A 40 "FATAL EXCEPTION"`.
- Compose and Room exceptions can be long. Read from `FATAL EXCEPTION` or `Caused by:` down to the first `com.felix.greengriffin` frame.
- On an ANR, grab `adb shell dumpsys activity anr` right away. `adb bugreport build/test-on-device/bugreport.zip` has everything, but it is slow and large.

## Process and app state

```bash
adb shell pidof -s com.felix.greengriffin                           # empty = not running
adb shell am start -n com.felix.greengriffin/.MainActivity          # launch
adb shell am start -W -n com.felix.greengriffin/.MainActivity       # launch + startup timing (TotalTime)
adb shell am force-stop com.felix.greengriffin                      # hard stop (like swipe-away)
adb shell dumpsys activity activities | grep -A 3 greengriffin      # resumed activity / task
adb shell dumpsys package com.felix.greengriffin | grep -E "versionName|lastUpdateTime|pkgFlags"
```

**Process death (tests saved-state restore):** put the app in the background, kill it, bring it back.
```bash
adb shell input keyevent 3                         # HOME
adb shell am kill com.felix.greengriffin           # only kills background processes
adb shell am start -n com.felix.greengriffin/.MainActivity
```
Unlike `force-stop`, this keeps the task, so Android recreates the activity and the back stack (Navigation 3 routes are saved). Then check that the board, the hand and the points match what you saw before, both in the UI and in the `game_state` row.

## Configuration changes

Reset each of these when you are done.

```bash
# Rotation
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1     # 0 portrait, 1 landscape, 2, 3
adb shell settings put system accelerometer_rotation 1   # restore auto-rotate

# Dark mode
adb shell cmd uimode night yes    # no / auto to restore

# Font scale (text overflow on stones/board)
adb shell settings put system font_scale 1.3      # restore: 1.0

# Faster, deterministic UI (disable animations while testing)
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0   # restore all three to 1

# Per-app language (dictionary is de/sv)
adb shell cmd locale set-app-locales com.felix.greengriffin --locales de-DE   # --locales "" to reset
```

## Database (Room)

`dictionary.db` holds the bundled dictionary (about 29 MB) and the user data in the `game_state` and `completed_levels` tables. The app opens it in WAL mode, so recent writes may exist only in `dictionary.db-wal`. `pull-db.sh` copies all three files, and `query-db.py` reads them together.

```bash
db=$(bash .claude/skills/test-on-device/scripts/pull-db.sh | tail -1)
py .claude/skills/test-on-device/scripts/query-db.py "$db"                         # overview
py .claude/skills/test-on-device/scripts/query-db.py "$db" "SELECT * FROM completed_levels"
py .claude/skills/test-on-device/scripts/query-db.py --width 0 "$db" \
  "SELECT game_state_json FROM game_state WHERE game_mode_id=1"                    # full JSON
py .claude/skills/test-on-device/scripts/query-db.py "$db" \
  "SELECT COUNT(*) FROM dictionary WHERE language='de' AND word='haus'"            # is a word known?
```

- To compare before and after an action, pull twice (each pull gets its own timestamped folder) and diff the query output.
- Room schema check: `SELECT identity_hash FROM room_master_table`.
- Words in the `dictionary` table are stored in lowercase. `DictionaryDao` expects lowercase input, while the board shows letters in uppercase.
- To list files: `adb shell run-as com.felix.greengriffin ls -la databases files shared_prefs cache`.

### Restore a database backup

Only restore into an app with the same Room `version`. Force-stop the app first so it doesn't hold the DB open:
```bash
adb shell am force-stop com.felix.greengriffin
b=build/test-on-device/db/backup-before-uninstall
for f in dictionary.db dictionary.db-wal dictionary.db-shm; do
  [ -f "$b/$f" ] && adb shell "run-as com.felix.greengriffin sh -c 'cat > databases/$f'" < "$b/$f"
done
# A backup without a -wal file must not be combined with the device's stale WAL:
[ -f "$b/dictionary.db-wal" ] || adb shell run-as com.felix.greengriffin rm -f databases/dictionary.db-wal databases/dictionary.db-shm
```
Then launch the app and check the overview query.

### Log the SQL that Room runs

The framework SQLite logs statements for debuggable apps when this property is set. Restart the app afterwards:
```bash
adb shell setprop log.tag.SQLiteStatements VERBOSE
adb shell am force-stop com.felix.greengriffin && adb shell am start -n com.felix.greengriffin/.MainActivity
adb logcat -d -s SQLiteStatements:V
adb shell setprop log.tag.SQLiteStatements ""   # turn off
```
(`SQLiteTime` also logs how long each statement took.)

## Performance and rendering

```bash
adb shell dumpsys gfxinfo com.felix.greengriffin reset   # before the interaction
adb shell dumpsys gfxinfo com.felix.greengriffin         # after: janky frames, percentiles
adb shell dumpsys meminfo com.felix.greengriffin         # memory (e.g. leaks after repeated navigation)
```
Compose recomposition counts are not available over adb. For that, add a temporary `GGDBG` log inside a `SideEffect { }` in the composable you suspect.

## Evidence capture

```bash
android screen capture -o build/test-on-device/<step>.png            # screenshot (always look at it)
android screen capture --annotate -o build/test-on-device/<step>-annotated.png
adb shell screenrecord --time-limit 20 /sdcard/gg.mp4                # record a flaky interaction
adb pull /sdcard/gg.mp4 build/test-on-device/ && adb shell rm /sdcard/gg.mp4
```
You can't watch a video yourself. Record one only as evidence for the user, and use screenshots for your own checks.

## Input cheatsheet

```bash
adb shell input tap X Y
adb shell input swipe X1 Y1 X2 Y2 600                 # scroll, slow
adb shell input draganddrop X1 Y1 X2 Y2 1500           # drag a stone onto the board
adb shell input motionevent DOWN X Y; adb shell input motionevent MOVE X2 Y2; adb shell input motionevent UP X2 Y2
adb shell input text "HAUS"                            # focus the field first
adb shell input keyevent 4                             # BACK   (3 = HOME, 66 = ENTER, 187 = RECENTS)
```
