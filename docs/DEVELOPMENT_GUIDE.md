# Development guide

## First-time setup

1. Install Android Studio, SDK 37, Build Tools 36 and JDK 17+.
2. `copy local.properties.example local.properties`; fill in Supabase values when you have a project (optional for UI work).
3. Open the folder in Android Studio and let Gradle sync. Select the **devDebug** build variant.

## Daily commands

| Task | Command |
|---|---|
| Build + install | `.\gradlew.bat installDevDebug` |
| Unit tests | `.\gradlew.bat testDevDebugUnitTest testDebugUnitTest :core:domain:test` |
| UI tests (device) | `.\gradlew.bat :core:designsystem:connectedDebugAndroidTest :feature:profile:connectedDebugAndroidTest` |
| Release build | `.\gradlew.bat assembleProdRelease` (unsigned unless you configure signing) |
| Logs | `adb logcat -s MS/App MS/Network MS/Auth MS/Database` |

## Flavors and secrets

- `dev`: application id suffix `.dev`; reads `SUPABASE_URL`, `SUPABASE_ANON_KEY`.
- `prod`: reads `PROD_SUPABASE_URL`, `PROD_SUPABASE_ANON_KEY`. Use a separate Supabase project.
- Values come from `local.properties` (git-ignored) or environment variables. The build fails if a secret/service_role key is supplied.
- Debug builds allow cleartext HTTP only to `10.0.2.2` / `localhost` for a local Supabase stack; release builds are HTTPS only.

## Adding a feature (example: Notifications)

1. Add models/repository interface/use cases to `core:domain` (with unit tests).
2. Add `data/notification` (data source, DTOs, mapper, repository impl, Hilt module) and include it in `settings.gradle.kts` and `app/build.gradle.kts`.
3. Add `feature/notifications` using the `musicsocial.android.feature` plugin; screen + ViewModel + UiState; export a `notificationsSection()` nav function and register it in `AppNavigation`.
4. Add routes to `core:navigation`. Add strings to the feature's `strings.xml`.

## Replacing sample data

`core:sample` feeds the screens until real repositories exist. For each ViewModel: inject the repository, map `AppResult` to
UI state (loading / content / error), delete the `SampleData` usage. When no ViewModel imports `SampleData`, remove the
`core:sample` module and its `implementation(projects.core.sample)` lines.

## Release signing (not configured)

Create a keystore outside the repo, reference it from a git-ignored `keystore.properties`, and add a `signingConfigs.release`
block in `app/build.gradle.kts`. Enrol in Play App Signing. Replace the placeholder application id first.

## Backend

See `backend/supabase/README.md`.
