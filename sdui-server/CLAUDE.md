# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run the server (default port 8080)
./gradlew bootRun

# Run all tests + generate JaCoCo coverage report
./gradlew test

# Run a single test class
./gradlew test --tests "com.thinkuldeep.sdui.server.controller.LandingPageControllerTest"

# Run a single test by name
./gradlew test --tests "com.thinkuldeep.sdui.server.controller.LandingPageControllerTest.landingPage returns a Column as root component"

# View coverage report (after running tests)
open build/reports/jacoco/test/html/index.html
```

## Architecture

This is a **Server-Driven UI (SDUI)** Spring Boot server. The server returns JSON-serialized UI component trees that clients (e.g., a KMP mobile app) render natively — layout and content are driven by the server response, not hardcoded on the client.

### UI Component Model (`model/UiComponent.kt`)

`UiComponent` is a sealed interface with a `type` discriminator field. Jackson serializes/deserializes it as a polymorphic type via `@JsonTypeInfo` / `@JsonSubTypes`. The supported types are:

| Type | Class | Purpose |
|------|-------|---------|
| `column` | `Column` | Vertical layout container |
| `row` | `Row` | Horizontal layout container |
| `text` | `Text` | Text with `size` and `weight` |
| `image` | `Image` | Image with `url`, `width`, `height` |
| `button` | `Button` | Button with `id`, `value`, `action` |
| `featuredItems` | `FeaturedItems` | Scrollable feature section with a `button` and child components |

### API Endpoints

- `GET /api/ui/landing` — returns the full landing page as a `Column` tree

### Key Infrastructure

- **`CorsConfig`** — allows all origins/methods/headers (open CORS for client development)
- **`HttpLoggingFilter`** — logs every HTTP request with method, URI, status, and duration

### Testing

Tests use `@WebMvcTest` (slice test — no full Spring context) with `MockMvc` for HTTP-level assertions plus direct controller instantiation for unit-level assertions. The `@WebMvcTest` import is `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (Spring Boot 4.x package).

JaCoCo runs automatically after `./gradlew test` and produces HTML + XML reports under `build/reports/jacoco/`.