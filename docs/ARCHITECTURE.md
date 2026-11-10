# Architecture

## Decisions and reasons

| Decision | Reason |
|---|---|
| Multi-module Gradle project | Enforces dependency rules at compile time, keeps builds incremental, lets features be developed independently. |
| One module per bounded context in `data/`, one per screen area in `feature/` | Matches ownership; avoids a giant "data" module. Modules only exist where code exists (no empty placeholders). |
| `core:domain` is a **pure Kotlin/JVM** module | Business rules and repository contracts cannot import Android, Supabase or Room, and their tests run in milliseconds. |
| The top-level `domain/` folder from the original plan is merged into `core:domain` | Two separate domain modules would split one layer for no benefit. |
| Features hold presentation only (`presentation/screen|components|viewmodel|state`) | Domain and data live in shared modules (`core:domain`, `data:*`) because features share models (Song, UserSummary). Per-feature domain/data packages would duplicate them. |
| Repository **interfaces** in `core:domain`, implementations in `data:*`, bound by Hilt in each data module | Dependency inversion: ViewModels depend on interfaces, so Supabase can be swapped or faked in tests. |
| Supabase accessed only from `data:*` through `core:network` | The UI and ViewModels never see the SDK. |
| `AppResult` / `AppError` instead of exceptions | Failures are part of the type signature; the UI maps `AppError` to user text in one place (`core:ui`). |
| Convention plugins in `build-logic/` | One definition of SDK levels, Java target, Compose, Hilt, Room and test dependencies. |
| Type-safe Navigation routes in `core:navigation` | Features navigate to each other through route types without depending on each other. |

## Modules and dependency rules

```
app ──► feature:* ──► core:ui ──► core:designsystem
 │          │            └──────► core:domain
 │          └────────► core:domain, core:navigation, core:common
 └──► data:* ──► core:network, core:database, core:common, core:domain
```

Rules:
1. `core:domain` and `core:navigation` depend on nothing in the project (pure Kotlin).
2. `feature:*` never depends on another feature or on `data:*`.
3. `data:*` never depends on `feature:*` or on `app`.
4. Only `app` depends on every data module (it wires Hilt bindings).
5. `core:designsystem` knows nothing about domain models.

| Module | Responsibility |
|---|---|
| `app` | `MainActivity`, `Application`, navigation shell, bottom bar, `BackendConfig` from `BuildConfig` |
| `core:common` | Logging (`AppLogger`, `LogRedactor`), coroutine dispatchers, connectivity, `Clock` |
| `core:designsystem` | Theme (colour, type, shape, spacing), icons, reusable components |
| `core:ui` | Domain-aware UI helpers: `AppError` -> message, screen header, formatters |
| `core:domain` | Models, repository interfaces, use cases, `AppResult`/`AppError` |
| `core:navigation` | Serializable route types |
| `core:network` | OkHttp client, Supabase client provider, exception -> `AppError` mapping |
| `core:database` | Room database, DAOs, entities |
| `core:datastore` | Preferences DataStore (theme mode) |
| `core:sample` | **Testing only**: hand-written sample content. Delete when repositories are real |
| `core:testing` | Fakes and test rules (`MainDispatcherRule`, `FakeAuthRepository`, ...) |
| `data:auth` | Supabase Auth data source + `AuthRepository` implementation |
| `data:music` | Catalogue: Supabase source + Room cache + `MusicRepository` (reference implementation) |
| `data:user`, `data:chat` | Remote contracts, DTOs, mappers. Implementations are Phase 2 / Phase 4 |
| `feature:*` | Screens and ViewModels |

## Data flow

```
Composable ──events──► ViewModel ──► UseCase / Repository interface (core:domain)
    ▲                      │                         │ implemented in data:*
    └───── StateFlow<UiState> ◄────────  AppResult   ▼
                                           RemoteDataSource (Supabase)  +  Room cache
```

## State management

- Each screen has an immutable `UiState` data class exposed as `StateFlow` from a `@HiltViewModel`.
- Screens collect it with `collectAsStateWithLifecycle()`. Composables are stateless: `FooRoute` (stateful, injects the ViewModel) calls `FooScreen(state, callbacks)` (stateless, previewable).
- One-off events use `SharedFlow`/channels only where needed (none yet).
- `stateIn(WhileSubscribed(5_000))` stops upstream work shortly after the screen leaves the foreground.

## Repository pattern (music as the reference)

`MusicRepositoryImpl`: fresh cache hit -> no network; miss -> fetch and cache; network failure with a stale copy -> serve stale;
cache errors are logged and treated as a miss so a broken cache never breaks the feature.

## Cross-cutting

- **Errors:** data sources return `AppResult`; `SupabaseClientProvider.call` converts exceptions (never swallowing coroutine cancellation).
- **Logging:** `AppLogger` -> Timber; every message passes `LogRedactor` (JWTs, bearer tokens, passwords, API keys, emails). `CrashReporter` is a seam with a no-op binding.
- **Security:** only the publishable key is in the app; the build rejects secret/service_role keys; RLS protects every table; backups of local data are disabled.
