# Project structure

```
Butter/
├── app/                      Application module (Hilt entry, navigation shell, flavors dev/prod)
├── build-logic/convention/   Gradle convention plugins (android app/library/compose/feature/room, hilt, jvm)
├── core/
│   ├── common/               logging, dispatchers, connectivity
│   ├── database/             Room
│   ├── datastore/            Preferences DataStore
│   ├── designsystem/         theme, icons, components
│   ├── domain/               models, repository interfaces, use cases (pure Kotlin)
│   ├── navigation/           type-safe route definitions (pure Kotlin)
│   ├── network/              OkHttp + Supabase client + error mapping
│   ├── sample/               TESTING ONLY sample content
│   ├── testing/              fakes and test rules
│   └── ui/                   domain-aware UI helpers
├── data/{auth,chat,music,user}/
├── feature/{chat,discover,home,player,profile}/
│       └── presentation/{screen,components,viewmodel,state}   (+ navigation/)
├── backend/supabase/         config.toml, migrations/, seed.sql, tests/
├── docs/
├── gradle/libs.versions.toml version catalog
├── settings.gradle.kts, build.gradle.kts, gradle.properties
└── local.properties.example
```

## Differences from the original plan

| Planned | Now | Why |
|---|---|---|
| top-level `domain/` | merged into `core:domain` | One domain layer; two modules add nothing |
| feature modules `onboarding`, `music`, `playlist`, `social`, `notifications`, `settings` | not created yet | No code to put in them; create when the feature is built |
| data modules `playlist`, `social`, `notification` | not created yet | Same reason; `data:user` covers follows until social grows |
| per-feature `domain/` and `data/` packages | shared `core:domain` and `data:*` | Models are shared across features |
| `app/di/AppModule.kt`, `navigation/{AppNavigation,Screen,BottomNavigation}.kt` | as planned | |
| additional | `core:sample`, `build-logic/` | sample data for testing; shared build configuration |

Package root: `com.yourcompany.musicsocial` (placeholder). **The applicationId cannot change after the first Play Store
release**, so replace it (and the package) before publishing.
