# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run the server (default port 8080)
./gradlew bootRun

# Run with OpenTelemetry instrumentation (exports traces to http://localhost:4318/v1/traces)
# 1. First, download the agent JAR:
java -cp "build/libs/*" -Dotel.exporter.otlp.endpoint=http://localhost:4318 -javaagent:~/.m2/repository/io/opentelemetry/javaagent/opentelemetry-javaagent/2.2.0/opentelemetry-javaagent-2.2.0.jar -Dspring.devtools.restart.enabled=false -Dfile.encoding=UTF-8 org.springframework.boot.loader.launch.JarLauncher

# Or set environment variables and use Gradle:
# OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 ./gradlew bootRun

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

The server exports traces to Jaeger via the OTLP (OpenTelemetry Protocol) HTTP endpoint at `http://localhost:4318/v1/traces`. 

**Configuration:** `OtelConfig.kt` defines a parent-based sampler with 1% base sampling. The exporter is configured via environment variables in `application.properties`:
- `otel.exporter.otlp.endpoint=http://localhost:4318`
- `otel.traces.exporter=otlp`
- `otel.service.name=sdui-server`

**Automatic Instrumentation:** To automatically create spans for HTTP requests and other operations, run the server with the OpenTelemetry Java agent:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
./gradlew bootRun -Dorg.springframework.boot.devtools.restart.trigger-file=.springBoot
```

Or manually download and use the agent JAR. Without the agent, only trace context propagation works (trace IDs flow through requests but no spans are created on the server).