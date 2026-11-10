# Coding standards

## Kotlin and Compose
- Official Kotlin style. PascalCase types, camelCase members, `UPPER_SNAKE` constants. Explicit visibility: `internal` by default inside modules, `public` only for what other modules use.
- No business logic in composables. Composables take state and callbacks; `FooRoute` wires the ViewModel, `FooScreen` is stateless and has a `@Preview`.
- Use `collectAsStateWithLifecycle()`, stable keys in lazy lists, `@Immutable` UI state, and read animated values inside `graphicsLayer`/draw lambdas to avoid recomposition.
- Colours, spacing, shapes and icons come from `core:designsystem` (`ButterTheme`, `Spacing`, `AppShapes`, `AppIcons`). No literal hex colours or dp spacing in features.
- Every user-visible string is in `strings.xml`. Every icon that conveys meaning has a content description; decorative icons use `null`.
- Touch targets are at least 44 dp.

## Architecture rules
- ViewModels depend on repository interfaces or use cases, never on Supabase, Room or `data:*`.
- Return `AppResult`; do not throw for expected failures. Never catch `CancellationException` without rethrowing.
- Inject dispatchers (`@IoDispatcher`) and `Clock`; no `Dispatchers.IO` or `Instant.now()` in logic that needs testing.
- Use structured concurrency (`viewModelScope`, `@ApplicationScope` for work that must outlive a screen). No `GlobalScope`.

## Security and logging
- No secrets in source. Only the publishable key ships in the app.
- Log through `AppLogger` with a `LogCategory`. Never log passwords, tokens or emails (the redactor is a safety net, not permission).
- Validate input before sending (use cases) and rely on database constraints and RLS as the real enforcement.

## Dependencies
- Add versions only to `gradle/libs.versions.toml`; pin exact versions; prefer maintained libraries; justify new dependencies in the PR.

## Tests
- Test behaviour, not coverage: use cases (rules), ViewModels (state transitions with Turbine + `MainDispatcherRule`), repositories (cache policy, error mapping), mappers (edge cases), and Compose UI for interactions.
- Use fakes from `core:testing` before mocks.

## Git
- Small commits with imperative messages; no secrets; never commit `local.properties` or keystores.
