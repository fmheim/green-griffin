# Green Griffin

Android word-building game (Kotlin, Jetpack Compose, Hilt, Room, Navigation 3). Single `:app` module, package `com.felix.greengriffin`.

Game modes (`board/presentation/GameState.kt` → `sealed interface GameMode`):
- **FreePlay**: classic tile placement on a board with drag-and-drop stones and jokers.
- **Trails**: level-based; build words from start fields to goal fields around blocked fields. Levels are defined in code (`trailLevels`).

## Build & run

```bash
./gradlew assembleDebug      # build debug APK
./gradlew installDebug       # install on connected device/emulator
./gradlew testDebugUnitTest  # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (needs device)
./gradlew lintDebug
```

- On Windows PowerShell use `.\gradlew.bat`; in Git Bash `./gradlew` works.
- Toolchain: AGP 9.x with built-in Kotlin (no separate `kotlin-android` plugin), Gradle 9.7, KSP for Hilt and Room. JVM target 17. `minSdk 33`, `targetSdk 36`, `compileSdk 37`.
- All versions live in `gradle/libs.versions.toml`; add dependencies there, never inline.
- `local.properties` must contain `apiKey=...` (read by the secrets Gradle plugin into `BuildConfig.apiKey`). Never commit or print it.
- Release builds are minified with R8 (`app/proguard-rules.pro`).
- In Claude Code cloud sessions the Android SDK and `local.properties` are provisioned by `.claude/scripts/cloud-setup.sh` (the environment's setup script) and `.claude/hooks/session-start.sh`. The environment must use **Custom** network access including `dl.google.com`, or nothing Android resolves. See `docs/claude-cloud-environment.md`.

## Package layout

Packages are organized **feature-first**, each with its own layers:

```
com.felix.greengriffin/
├── MainActivity.kt        # Navigation 3 NavDisplay + all routes (NavKeys)
├── HomeScreen.kt
├── GreenGriffinApp.kt     # @HiltAndroidApp
├── board/                 # core board game (shared by FreePlay and Trails)
│   ├── data/local/        # Room: AppDatabase, entities, DAOs
│   ├── data/mapper/
│   ├── data/repository/   # LocalWordRepository, GameStateRepository, CompletedLevelsRepository
│   ├── domain/            # WordRepository interface
│   ├── domain/usecase/    # AreWordsValidUseCase, IsPlacementValidUseCase
│   └── presentation/      # WordPlacementScreen/ViewModel, GameState, GameEvent, components/
├── trails/
│   ├── domain/usecase/
│   └── presentation/levels/   # TrailLevelsScreen (in TrailLevels.kt) + ViewModel
├── core/presentation/     # theme/, icons/
├── di/                    # AppModule, DatabaseModule, WordValidationModule
└── util/extensions/
```

New features go in their own top-level package (`feature/{data,domain,presentation}`), like `trails/`. Only `WordRepository` currently has a domain interface; other repositories are concrete classes in `data/` — follow the existing pattern unless you are deliberately introducing an interface.

## Architecture conventions

### Presentation (MVVM with events)
- ViewModels expose a single `StateFlow<State>` (`private val _state = MutableStateFlow(...)`, `val state = _state.asStateFlow()`) and accept UI actions via one `fun onEvent(event: XEvent)` with a `sealed interface` of events (see `GameEvent`).
- Screens are stateless: `XScreen(state, onEvent/on... callbacks, modifier)`. The ViewModel is obtained and collected in the `entry<Route>` block in `MainActivity`, not inside the screen.
- Collect state with `collectAsStateWithLifecycle()` (not `collectAsState()`).
- Reusable composables have no suffix; full screens end in `Screen`. Accept `modifier: Modifier = Modifier` as the first optional parameter.

### Navigation 3
- Routes are `@Serializable` types implementing `NavKey`, declared in `MainActivity.kt`. Navigate with `backStack.add(route)` / `backStack.removeLastOrNull()`.
- `rememberViewModelStoreNavEntryDecorator` scopes a ViewModel to each back-stack entry, so every route instance gets its own ViewModel.
- ViewModels that need the route's arguments use Hilt **assisted injection**; copy `WordPlacementViewModel`:
  ```kotlin
  @HiltViewModel(assistedFactory = MyViewModel.Factory::class)
  class MyViewModel @AssistedInject constructor(
      private val someUseCase: SomeUseCase,
      @Assisted val navKey: RouteToMyScreen,
  ) : ViewModel() {
      @AssistedFactory interface Factory { fun create(navKey: RouteToMyScreen): MyViewModel }
  }
  // in entry<RouteToMyScreen> { key -> hiltViewModel<MyViewModel, MyViewModel.Factory> { it.create(key) } }
  ```
  ViewModels without arguments use plain `@HiltViewModel` + `@Inject constructor` and `hiltViewModel<T>()`.

### Domain
- Use cases: `XUseCase @Inject constructor(...)` with `operator fun invoke(...)`; no Android imports. Results are modeled as sealed interfaces (e.g. `WordValidation`, `PlacementValidation`).

### DI (Hilt)
- `AppModule`: `GenerativeModel` (Gemini, currently unused by app code).
- `DatabaseModule`: `AppDatabase` singleton and DAOs.
- `WordValidationModule`: `@Binds LocalWordRepository → WordRepository`.
- Classes with an `@Inject` constructor need no module entry; add `@Binds` only for interface → implementation.

## Database (Room) — read before changing entities

`AppDatabase` is a **single database file** (`dictionary.db`) holding both the bundled dictionary and user data:
- `DictionaryWord` — words with `language` (`"de"`, `"sv"`), pre-populated via `createFromAsset("dictionary.db")` from `app/src/main/assets/dictionary.db`.
- `GameStateEntity` — saved games per game mode / trail level.
- `CompletedLevelEntity` — Trails progress.

It is built with `fallbackToDestructiveMigration(dropAllTables = true)` and `exportSchema = false`. Consequences:
- Bumping `version` without a `Migration` **wipes all saved games and level progress**, then re-copies the asset. Only do this if losing user data is acceptable; otherwise add a `Migration(n, n+1)` via `.addMigrations(...)`.
- Room validates that the asset's schema matches the entities. Changing `DictionaryWord` (or any table the asset contains) requires regenerating `assets/dictionary.db` with the matching schema and version.
- Before adding real migrations, enable schema export (Room Gradle plugin, `room { schemaDirectory("$projectDir/schemas") }`, `exportSchema = true`) and commit the `schemas/` JSON so migrations can be tested with `MigrationTestHelper`.

## AI (Gemini)

`AppModule` provides a `GenerativeModel` from the legacy `com.google.ai.client.generativeai` SDK (model `gemini-2.5-flash`, API key from `BuildConfig`). That SDK is **deprecated**; Google recommends migrating to Firebase AI Logic (`com.google.firebase:firebase-ai`) with App Check so the API key is not shipped in the APK. Don't build new features on the legacy SDK without flagging this. Word validation currently uses only the local dictionary.

## Testing

- Current setup is minimal: JUnit 4 (`app/src/test`), AndroidX JUnit + Espresso + Compose `ui-test-junit4` (`app/src/androidTest`); only placeholder tests exist.
- Highest-value targets are pure domain logic: `IsPlacementValidUseCase`, `AreWordsValidUseCase` (with a fake `WordRepository`), `CompleteTrailLevelIfGoalReachedUseCase`, and `GameStateMapper`.
- The `/android-testing` skill assumes JUnit 5, Turbine and AssertK, which are **not** in the project yet — add them to `libs.versions.toml` first (or ask) rather than assuming they exist.

## Git

- Base branch and PR target: `develop`.
- Commit messages use Conventional Commits in present tense: `feat: Add ...`, `fix: ...`, `build: Update ...`, `refactor: ...`.
- Branch names: `feature/...`, `fix/...`, `refactor/...`.

## Gotchas

- `.cursorrules` and `tools/*.py` are leftovers from a Cursor setup (Python venv, LLM/scraper scripts). Ignore them; they are not part of the Android build and their version notes are outdated.
- `UiState.kt` at the package root is an unused leftover from the Gemini sample template.
- The Compose opt-ins (`ExperimentalFoundationApi`, `ExperimentalMaterial3Api`, etc.) are enabled globally in `app/build.gradle.kts`; no `@OptIn` annotations needed.
- Project-local skills live in `.claude/skills/` (android-cli, compose-ui, data-layer, testing, r8-analyzer, ...). They are generic Android/KMP guides — where they conflict with the conventions above, follow this file. The exception is `/test-on-device`, which is project-specific: it installs the CI or local debug APK on a connected device and verifies or debugs changes there (logcat, pulled Room DB, temporary `GGDBG` logs).
