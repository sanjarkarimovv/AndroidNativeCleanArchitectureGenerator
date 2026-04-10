# Android Native Clean Architecture Generator

[![Kotlin](https://img.shields.io/badge/Kotlin-Script-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-Clean%20Architecture-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A professional Kotlin Script that generates a complete **monolith (single-module)** Android project with Clean Architecture structure. Inspired by [CleanArchitectureForAndroid](https://github.com/EranBoudjnah/CleanArchitectureForAndroid) and the book *"Clean Architecture for Android"* by Eran Boudjnah.

> Unlike multi-module generators, this tool creates a clean architecture setup within a **single module** using packages — ideal for small-to-medium projects, MVPs, and teams that prefer simplicity without sacrificing architectural discipline.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │  Composables, Activities
├─────────────────────────────────────────────────────────┤
│                 Presentation Layer                      │  ViewModels, States, Mappers
├─────────────────────────────────────────────────────────┤
│                   Domain Layer                          │  UseCases, Repository Interfaces
├─────────────────────────────────────────────────────────┤
│                    Data Layer                           │  Repository Implementations
├─────────────────────────────────────────────────────────┤
│                 DataSource Layer                        │  API, Database, Preferences
└─────────────────────────────────────────────────────────┘
```

**Data Flow:**
```
UI → Event → ViewModel → UseCase → Repository Interface
                ↓              ↓
         StateFlow<State>   Repository Impl → DataSource → API/DB
```

**Dependency Rule:** Inner layers know nothing about outer layers.

---

## Installation

### Option 1: Homebrew (macOS/Linux)

```bash
brew install kotlin
```

Then download the script:

```bash
curl -O https://raw.githubusercontent.com/sanjarkarimovv/AndroidNativeCleanArchitectureGenerator/main/setup-clean-architecture.kts
chmod +x setup-clean-architecture.kts
```

### Option 2: Manual

1. Install [Kotlin](https://kotlinlang.org/docs/command-line.html) (requires JDK 17+)
2. Clone this repository:

```bash
git clone https://github.com/sanjarkarimovv/AndroidNativeCleanArchitectureGenerator.git
cd AndroidNativeCleanArchitectureGenerator
```

---

## Usage

### Interactive Mode

Run without arguments — the script asks questions one by one:

```bash
kotlinc -script setup-clean-architecture.kts
```

```
╔══════════════════════════════════════════════════╗
║   Clean Architecture Monolith Setup              ║
╚══════════════════════════════════════════════════╝

Project name: MyApp
Package name (default: com.example.myapp): com.example.myapp
Min SDK version (default: 24): 24
Target SDK version (default: 35): 35
Compile SDK version (default: 35): 35

── Product Flavors ──
How many product flavors? (default: 2): 2
Flavor 1 name? (default: development): development
Flavor 1 app name? (default: MyApp(dev)): MyApp(dev)
Flavor 1 base URL?: https://dev-api.example.com/
Flavor 2 name? (default: production): production
Flavor 2 app name? (default: MyApp): MyApp
Flavor 2 base URL?: https://api.example.com/

── Optional Components ──
Include Retrofit + Kotlin Serialization? (y/N): y
Include Room database? (y/N): y
Include DataStore preferences? (y/N): n
Include KtLint? (y/N): y
Include Detekt? (y/N): y
```

### Flag Mode

Pass all options as arguments for automation:

```bash
kotlinc -script setup-clean-architecture.kts -- \
  --name "MyApp" \
  --package "com.example.myapp" \
  --min-sdk 24 \
  --target-sdk 35 \
  --flavors "development:MyApp(dev):https://dev-api.example.com/,production:MyApp:https://api.example.com/" \
  --retrofit \
  --room \
  --datastore \
  --ktlint \
  --detekt
```

### All Flags

| Flag | Description | Default |
|------|-------------|---------|
| `--name <name>` | Project name | *required* |
| `--package <pkg>` | Package name | *required* |
| `--min-sdk <sdk>` | Minimum SDK version | `24` |
| `--target-sdk <sdk>` | Target SDK version | `35` |
| `--compile-sdk <sdk>` | Compile SDK version | `35` |
| `--flavors <flavors>` | Product flavors (format below) | *required* |
| `--retrofit` | Include Retrofit + Kotlin Serialization | `false` |
| `--room` | Include Room database | `false` |
| `--datastore` | Include DataStore preferences | `false` |
| `--ktlint` | Include KtLint code formatting | `false` |
| `--detekt` | Include Detekt static analysis | `false` |
| `--help`, `-h` | Show help | - |

**Flavors format:** `name:appName:baseUrl,name:appName:baseUrl`

Example: `"development:MyApp(dev):https://dev.api.com/,production:MyApp:https://api.com/"`

---

## Generated Project Structure

```
MyApp/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/example/myapp/
│       │       ├── MyAppApplication.kt          # @HiltAndroidApp
│       │       ├── MainActivity.kt              # @AndroidEntryPoint + Compose
│       │       │
│       │       ├── architecture/
│       │       │   ├── domain/
│       │       │   │   ├── usecase/
│       │       │   │   │   ├── UseCase.kt                        # Base interface
│       │       │   │   │   ├── BackgroundExecutingUseCase.kt     # One-time IO operations
│       │       │   │   │   └── ContinuousExecutingUseCase.kt     # Flow-based operations
│       │       │   │   ├── exception/
│       │       │   │   │   ├── DomainException.kt
│       │       │   │   │   └── UnknownDomainException.kt
│       │       │   │   ├── UseCaseExecutor.kt
│       │       │   │   └── UseCaseExecutorProvider.kt
│       │       │   ├── presentation/
│       │       │   │   ├── viewmodel/
│       │       │   │   │   └── BaseViewModel.kt                  # STATE + NOTIFICATION flows
│       │       │   │   ├── navigation/
│       │       │   │   │   └── PresentationNavigationEvent.kt
│       │       │   │   └── notification/
│       │       │   │       └── PresentationNotification.kt
│       │       │   └── ui/
│       │       │       ├── navigation/
│       │       │       │   ├── mapper/NavigationEventDestinationMapper.kt
│       │       │       │   ├── model/UiDestination.kt
│       │       │       │   └── exception/UnhandledNavigationException.kt
│       │       │       ├── notification/
│       │       │       │   ├── mapper/NotificationUiMapper.kt
│       │       │       │   └── model/UiNotification.kt
│       │       │       ├── view/
│       │       │       │   ├── BaseComposeHolder.kt
│       │       │       │   ├── ScreenEnterObserver.kt
│       │       │       │   └── ViewsProvider.kt
│       │       │       └── binder/
│       │       │           └── ViewStateBinder.kt
│       │       │
│       │       ├── coroutine/
│       │       │   └── CoroutineContextProvider.kt    # Abstracts Main/IO dispatchers
│       │       │
│       │       ├── datasource/
│       │       │   ├── architecture/
│       │       │   │   └── exception/DataException.kt
│       │       │   ├── source/                        # DataSource interfaces (per feature)
│       │       │   ├── network/                       # (if --retrofit)
│       │       │   │   ├── AuthInterceptor.kt
│       │       │   │   └── ApiErrorHandler.kt
│       │       │   └── local/                         # (if --room / --datastore)
│       │       │       ├── AppDatabase.kt             # (if --room)
│       │       │       └── PreferencesManager.kt      # (if --datastore)
│       │       │
│       │       ├── analytics/
│       │       │   ├── Analytics.kt
│       │       │   ├── AnalyticsEvent.kt
│       │       │   ├── bogus/BogusAnalytics.kt
│       │       │   └── event/Click.kt
│       │       │
│       │       ├── navigation/mapper/                 # Navigation mappers (per feature)
│       │       ├── widget/                            # Shared composable widgets
│       │       │
│       │       ├── di/
│       │       │   ├── AppModule.kt                   # UseCaseExecutor provider
│       │       │   ├── ArchitectureModule.kt          # Coroutine + Analytics providers
│       │       │   ├── NetworkModule.kt               # (if --retrofit)
│       │       │   ├── DatabaseModule.kt              # (if --room)
│       │       │   └── DataStoreModule.kt             # (if --datastore)
│       │       │
│       │       ├── flavor/
│       │       │   └── AppFlavor.kt                   # Product flavor enum
│       │       │
│       │       └── feature/                           # Features go here
│       │
│       ├── dev/res/                                   # Development flavor resources
│       ├── prod/res/                                  # Production flavor resources
│       │
│       ├── test/java/com/example/myapp/
│       │   ├── architecture/presentation/viewmodel/
│       │   │   └── BaseViewModelTest.kt               # Test helpers for ViewModels
│       │   ├── coroutine/
│       │   │   ├── FakeCoroutineContext.kt
│       │   │   ├── FakeCoroutineContextProvider.kt
│       │   │   └── FlowCurrentValueReader.kt          # Flow.currentValue() extension
│       │   └── feature/                               # Unit & integration tests
│       │
│       └── androidTest/java/com/example/myapp/
│           ├── robot/                                 # Robot pattern base
│           └── feature/                               # UI & instrumented tests
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── wrapper/gradle-wrapper.properties
│   └── libs.versions.toml                             # Version catalog
├── .editorconfig
└── detekt.yml                                         # (if --detekt)
```

---

## What Gets Generated

### Always Included

| Component | Description |
|-----------|-------------|
| **Architecture Base Classes** | UseCase, BackgroundExecutingUseCase, ContinuousExecutingUseCase, UseCaseExecutor, BaseViewModel |
| **Navigation System** | Custom sealed interfaces — no framework dependency |
| **Notification System** | PresentationNotification + UiNotification mappers |
| **Coroutine Abstraction** | CoroutineContextProvider (injectable, testable) |
| **DataSource Architecture** | DataException base class |
| **Analytics** | Analytics interface + BogusAnalytics implementation |
| **Hilt DI** | AppModule, ArchitectureModule with all providers |
| **Product Flavors** | Customizable (default: development + production) |
| **Jetpack Compose** | Material3, Activity Compose, UI tooling |
| **Testing** | FakeCoroutineContext, BaseViewModelTest, Turbine, Mockito-Kotlin |
| **Gradle** | Version catalog, wrapper, build config |

### Optional (via flags)

| Flag | What It Adds |
|------|-------------|
| `--retrofit` | Retrofit + OkHttp + **Kotlin Serialization** + NetworkModule + AuthInterceptor |
| `--room` | Room database + AppDatabase + DatabaseModule |
| `--datastore` | DataStore Preferences + PreferencesManager + DataStoreModule |
| `--ktlint` | KtLint plugin + `.editorconfig` rules |
| `--detekt` | Detekt plugin + `detekt.yml` configuration |

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| DI | Hilt (Dagger) |
| Async | Kotlin Coroutines + Flow |
| Networking | Retrofit + OkHttp *(optional)* |
| Serialization | Kotlin Serialization |
| Database | Room *(optional)* |
| Preferences | DataStore *(optional)* |
| Testing | JUnit, Mockito-Kotlin, Turbine, Espresso, UIAutomator |
| Code Quality | KtLint *(optional)*, Detekt *(optional)* |
| Build | Gradle (Kotlin DSL) + Version Catalog |

---

## Product Flavors

The script generates an `AppFlavor` enum with customizable flavors. Default setup: **development** + **production**.

Each flavor includes:
- `applicationIdSuffix` (e.g., `.dev` — except production)
- `versionNameSuffix` (e.g., `-development` — except production)
- `appName` via `resValue`
- `BASE_URL` via `buildConfigField`
- Per-flavor resource directories

You can add any number of flavors (staging, QA, etc.) during setup.

---

## Layer Dependencies

| Layer | Can Depend On |
|-------|---------------|
| UI | Presentation, Architecture:UI |
| Presentation | Domain, Architecture:Presentation |
| Domain | Architecture:Domain **(NO Android SDK!)** |
| Data | Domain, DataSource |
| DataSource | Architecture:DataSource |

---

## After Generation

Once the project is generated:

1. `cd YourProjectName`
2. Open in Android Studio
3. Sync Gradle
4. Run the app

To add features, create packages under `feature/` following this structure:

```
feature/
└── home/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   └── usecase/
    ├── presentation/
    │   ├── model/
    │   ├── mapper/
    │   ├── viewmodel/
    │   └── navigation/
    ├── data/
    │   ├── repository/
    │   └── mapper/
    └── ui/
        ├── view/
        ├── model/
        ├── mapper/
        └── di/
```

---

## Credits

- Architecture patterns from [Clean Architecture for Android](https://github.com/EranBoudjnah/CleanArchitectureForAndroid) by [Eran Boudjnah](https://github.com/EranBoudjnah)
- Book: *"Clean Architecture for Android"* by Eran Boudjnah (BPB Publications)
- Multi-module generator: [CleanArchitectureGenerator](https://github.com/EranBoudjnah/CleanArchitectureGenerator)

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
