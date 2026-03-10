# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build Android APK
./gradlew :composeApp:assembleDebug

# Run Web (Wasm — modern browsers, faster)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run Web (JS — broader browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run tests
./gradlew :composeApp:allTests

# iOS: open iosApp/ directory in Xcode
```

## Architecture

This is a **Kotlin Multiplatform (KMP)** client for the SDUI server. It fetches a JSON UI component tree from the server and renders it natively using **Compose Multiplatform**. Targets: Android, iOS (arm64 + simulator), Web (Wasm + JS).

### Data Flow

1. `LandingViewModel` launches a coroutine → `UiRepository.fetchLanding()` → `GET /api/ui/landing`
2. Response is deserialized into `UiComponent` sealed classes (Kotlinx Serialization)
3. ViewModel stores result in `MutableStateFlow<UiComponent?>`
4. Platform entry point (Android: `MainActivity`, iOS: `MainViewController`) collects state and calls `Render()`
5. `Render()` is a recursive `@Composable` that pattern-matches on `UiComponent` subtypes

### UI Component Model (`commonMain/model/UiComponent.kt`)

Mirrors the server model. Uses Kotlinx Serialization with `@SerialName` for each subtype and `"type"` as the polymorphic discriminator field.

| Type | Class | Renders As |
|------|-------|------------|
| `column` | `Column` | `Column` composable |
| `row` | `Row` | `Row` composable |
| `text` | `Text` | `Text` with size/weight mapping |
| `image` | `Image` | `KamelImage` (async network image) |
| `button` | `Button` | `Button` with action dispatch |
| `featuredItems` | `FeaturedItems` | Carousel (one item visible, button rotates through children) |

### Key Files

- `commonMain/renderer/UiRenderer.kt` — recursive `Render()` composable
- `commonMain/viewmodel/LandingViewModel.kt` — state, coroutine fetch, action dispatch
- `commonMain/network/UiRepository.kt` — single `fetchLanding()` method
- `commonMain/network/HttpClientFactory.kt` — Ktor client with polymorphic JSON config
- `androidMain/PlatformConfig.kt` — base URL `http://10.0.2.2:8080` (emulator localhost)
- `iosMain/PlatformConfig.kt` — base URL `http://localhost:8080`

### Adding a New Component Type

1. Add sealed subclass to `UiComponent.kt` (client) with `@Serializable` + `@SerialName`
2. Add a matching branch in `Render()` in `UiRenderer.kt`
3. Add the corresponding type to the server's `UiComponent.kt` with `@JsonSubTypes` entry
4. Add server-side construction in `LandingPageController.kt`

### HTTP Client Configuration

`HttpClientFactory` configures Ktor with `ContentNegotiation` using Kotlinx JSON with:
- `classDiscriminator = "type"` (matches server's Jackson discriminator)
- `ignoreUnknownKeys = true` (forward compatibility)
- Polymorphic serializers registered for `UiComponent`