# Claude Code cloud environment for Green Griffin

How to configure a [cloud environment](https://code.claude.com/docs/en/cloud-environments)
so Claude Code cloud sessions can actually build this app and run Gradle tasks.

Out of the box they cannot, for one reason: **`dl.google.com` is not on the
Trusted allowlist.** Both the Android SDK
(`dl.google.com/android/repository/`) and Google's Maven repo
(`maven.google.com`, which is a 301 redirect to
`dl.google.com/dl/android/maven2/`) live there, so without it neither the SDK
nor AGP, AndroidX, Hilt or Room can be fetched. Adding that one domain is the
part that matters most here.

## One-time setup

### 1. Create the environment

At [claude.ai/code](https://claude.ai/code), open environment settings and
select **Add cloud environment**.

Name it something like `green-griffin-android`.

### 2. Network access: Custom

Set **Network access** to **Custom**, check **Also include default list of
common package managers**, and put this in **Allowed domains**:

```
dl.google.com
api.foojay.io
```

* `dl.google.com` — the Android SDK, and the real host behind `maven.google.com`.
  Without it nothing Android resolves.
* `api.foojay.io` — used by the `foojay-resolver-convention` plugin in
  `settings.gradle.kts` to provision JVM toolchains. Cloud sessions ship
  OpenJDK 21 and the build only asks for Java 17 source/target compatibility,
  so nothing should need downloading; include it so a future `jvmToolchain`
  change does not fail with a confusing error.

The defaults already cover `services.gradle.org`, `plugins.gradle.org`,
`repo1.maven.org`, `kotlinlang.org` and `developer.android.com`, all of which
this build uses.

### 3. Setup script

Paste the contents of [`.claude/scripts/cloud-setup.sh`](../.claude/scripts/cloud-setup.sh)
into the **Setup script** field. It:

1. resolves the current command-line tools build from `developer.android.com`
   (falling back to a pinned build) and installs them to `/opt/android-sdk`,
2. accepts the SDK licences,
3. installs `platform-tools`, `platforms;android-<compileSdk>` — read from
   `app/build.gradle.kts`, currently 37 — and the newest stable `build-tools`,
4. writes `/etc/profile.d/android-sdk.sh`,
5. warms the Gradle distribution and plugin caches in `/root/.gradle`.

The platform snapshots the filesystem after it finishes, so later sessions
start with all of that already on disk and skip the script. It re-runs when you
edit the script or the allowed domains, and about every seven days.

### 4. Environment variables (optional)

```
GREEN_GRIFFIN_API_KEY=...
```

Only needed if you want a working Gemini key in the session. Builds do **not**
need one: the secrets Gradle plugin just needs `apiKey` to exist in
`local.properties`, and the SessionStart hook writes a placeholder. Note that
anyone using the environment can read environment variables, so prefer leaving
this unset — on Pro and Max plans, store a real key as an
[API credential](https://code.claude.com/docs/en/cloud-environments#add-api-credentials)
instead.

## What happens per session

`.claude/hooks/session-start.sh` runs as a `SessionStart` hook, registered in
`.claude/settings.json`. It only acts when `CLAUDE_CODE_REMOTE=true`, so local
checkouts and Android Studio are unaffected. Each session it:

* locates the SDK and exports `ANDROID_HOME`, `ANDROID_SDK_ROOT` and the
  `PATH` additions through `$CLAUDE_ENV_FILE`,
* writes `local.properties` with `sdk.dir` and an `apiKey` — needed because
  `local.properties` is gitignored and so never part of the clone,
* prints the installed platforms and build-tools, and says plainly what to fix
  if the SDK is missing.

The hook runs **synchronously**: the session starts a little slower, but Claude
can never reach for a Gradle task before the SDK is wired up.

## Working commands

```bash
./gradlew testDebugUnitTest   # JVM unit tests
./gradlew lintDebug           # Android Lint
./gradlew assembleDebug       # debug APK
```

## Limitations

* **No emulator, no instrumented tests.** Cloud session VMs have no
  `/dev/kvm`, so `connectedDebugAndroidTest` cannot run. Rely on
  `testDebugUnitTest` and `lintDebug`; run instrumented tests locally or in CI
  with a device. The hook says so on startup.
* **Setup script budget is ~5 minutes.** The SDK download dominates it, so the
  Gradle warm-up is best-effort and bounded; the first real build in a fresh
  environment still resolves the app's own dependencies.
* **Memory.** VMs get roughly 4 vCPUs / 16 GB RAM / 30 GB disk.
  `gradle.properties` asks for `-Xmx2048m`, which fits comfortably.
