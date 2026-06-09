# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run the server (default port 8080)
./gradlew bootRun

# Run with OpenTelemetry agent for distributed tracing (requires Jaeger on localhost:4318)
./run-with-otel.sh

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

### OpenTelemetry & Distributed Tracing

The server exports traces to Jaeger via the OTLP (OpenTelemetry Protocol) HTTP endpoint. 

**Configuration:** 
- **Sampler:** Parent-based with 1% base sampling (respects parent trace sampling decision)
- **Error handling:** Tail-based error detection exports ALL error traces regardless of sampling
- **OTLP Endpoint:** Configured in `application.properties` (default: `http://localhost:4318/v1/traces`)
- **Service name:** `sdui-server`

**Running with Distributed Tracing:**

```bash
# 1. Start Jaeger (if not already running)
docker run -d --name jaeger \
  -p 6831:6831/udp -p 16686:16686 -p 4317:4317 -p 4318:4318 \
  jaegertracing/all-in-one:latest

# 2. Build the server
./gradlew build

# 3. Run with OpenTelemetry agent
./run-with-otel.sh
```

The `run-with-otel.sh` script:
- Auto-downloads OpenTelemetry Java agent (2.2.0)
- Finds the correct executable JAR
- Runs with agent for automatic HTTP span instrumentation
- Exports traces to Jaeger at `http://localhost:4318`

**Trace Features:**
- ✅ **Automatic HTTP instrumentation** — all requests create spans
- ✅ **Error override** — errors are exported even if parent sampled=00
- ✅ **Trace context propagation** — mobile client → server traces linked
- ✅ **Custom attributes** — slow requests, error flags, mobile sampling decision logged
- ✅ **Exception recording** — exceptions automatically added to spans

**View traces:** Open http://localhost:16686 after running server with agent.