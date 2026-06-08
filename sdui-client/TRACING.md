# OpenTelemetry Tracing Implementation

This document describes the OpenTelemetry (OTEL) tracing implementation for the SDUI client.

## Overview

The SDUI client now supports W3C Trace Context propagation, enabling distributed tracing across multiple services and platforms. The implementation automatically injects `traceparent` and `tracestate` headers into all HTTP requests.

## Key Components

### 1. TraceContext
Located in `commonMain/tracing/TraceContext.kt`

Data class that holds trace information:
- `traceId`: Unique identifier for the entire trace (16 hex characters)
- `spanId`: Identifier for the current operation (8 hex characters)
- `parentSpanId`: Optional identifier of the parent span
- `traceFlags`: Trace sampling flags (default: "01" = sampled)
- `traceState`: Vendor-specific trace state information
- `timestamp`: When the span was created

**Methods:**
- `toTraceparent()`: Formats the context as a W3C traceparent header value
- `toTracestate()`: Returns the tracestate header value
- `create()`: Static factory for creating new contexts

**ThreadLocal Holder:**
- `TraceContextHolder.set(context)`: Store context in thread-local storage
- `TraceContextHolder.current()`: Retrieve the current context
- `TraceContextHolder.clear()`: Clear the current context

### 2. TraceContextPropagator
Located in `commonMain/tracing/TraceContextPropagator.kt`

Handles extraction and injection of trace context:
- `extractContext(headers)`: Extract trace context from HTTP headers
- `injectContext(context, headers)`: Inject trace context into HTTP headers
- `createChildContext(parentContext)`: Create a child span with the same trace ID

### 3. TracingPlugin
Located in `commonMain/tracing/TracingPlugin.kt`

Ktor HTTP client plugin that:
- Automatically injects trace headers into all requests
- Logs request completion with trace information
- Associates trace context with each request

### 4. TraceSampler
Located in `commonMain/tracing/TraceSampler.kt`

Intelligent sampling based on environment and user type:
- **Production**: 1% of requests sampled
- **Staging**: 20% of requests sampled
- **QA**: 100% of requests sampled
- **QA Users**: Always sampled (even in Production)
- **Development**: 100% of requests sampled

**Sampling Decision**:
- QA users always sampled (highest priority)
- QA environment always sampled
- Staging environment samples 20%
- Production environment samples 1%
- Development environment always sampled

**Important**: Trace context is **always passed** in HTTP headers, even for non-sampled requests. This allows backend systems to make independent sampling decisions and trace correlation across services.

## Sampling Configuration

### Default Behavior

The sampler automatically determines sampling based on your build configuration:

```kotlin
// Default: Development environment, always sample
val sampler = TraceSampler() // 100% sampling
```

### Configure Sampling in ViewModel

```kotlin
val viewModel = LandingViewModel()

// Option 1: Configure for specific environment
viewModel.configureSampling(Environment.PRODUCTION)
// 1% of requests will be sampled

// Option 2: Configure with QA user flag
viewModel.configureSampling(Environment.STAGING, isQaUser = false)
// 20% of requests will be sampled

// Option 3: Configure QA user in production
viewModel.configureSampling(Environment.PRODUCTION, isQaUser = true)
// 100% of requests will be sampled

// Option 4: Configure with SamplingConfig object
val config = SamplingConfig(
    environment = Environment.QA,
    isQaUser = false
)
viewModel.configureSampling(config)
```

### Sampling Rates by Environment

| Environment | Sample Rate | Use Case |
|---|---|---|
| Production | 1% | High-volume production traffic, cost control |
| Staging | 20% | Pre-production validation, cost-effective testing |
| QA | 100% | Full regression testing, complete trace visibility |
| Development | 100% | Local development, debugging |
| QA Users | 100% | Special user tracking in any environment |

### Non-Sampled Requests

Even when a request is **not sampled** (`traceFlags = "00"`):
- The trace context is **still generated and passed**
- The `traceparent` header is **still sent** to the backend
- The backend can receive and process the trace context
- This allows correlation and independent sampling decisions

Example flow:
```
Client (Production, 99% non-sampled):
- Generates traceId: "abc123xyz"
- Sets traceFlags: "00" (not sampled)
- Sends header: "traceparent: 00-abc123xyz-def456-00"

Server receives:
- Extracts trace context from header
- Makes own sampling decision
- Can decide to sample if marked as important operation
- Or ignores based on tail sampling rules
```

## Usage

### Automatic Header Injection

The tracing plugin is automatically installed in `HttpClientFactory`. All HTTP requests will include:

```
traceparent: 00-<traceId>-<spanId>-01
tracestate: <optional-state>
```

Example:
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

### Setting Trace Context in ViewModel

```kotlin
val viewModel = LandingViewModel()

// Option 1: Set with traceparent and tracestate headers
viewModel.setTraceContext(
    traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
    tracestate = "vendor-specific-data"
)

// Option 2: Set with TraceContext object
val context = TraceContext.create(
    traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
    spanId = "00f067aa0ba902b7"
)
viewModel.setTraceContext(context)

// Retrieve current context
val currentContext = viewModel.getCurrentTraceContext()
println("Current Trace ID: ${currentContext?.traceId}")
```

### Setting Trace Context in Repository

```kotlin
val repository = UiRepository()

// Set trace context before making requests
repository.setTraceContext(
    traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
)

// Or set a TraceContext object
val context = TraceContext.create()
repository.setTraceContext(context)

// Check current context
val current = repository.getCurrentTraceContext()
```

### Manual Header Injection

For custom requests, you can manually inject trace headers:

```kotlin
val context = TraceContext.create()
val headers = mutableMapOf<String, String>()
TraceContextPropagator.injectContext(context, headers)

// Use headers in your request
client.get(url) {
    headers["traceparent"] = headers["traceparent"]!!
}
```

### Extracting Trace Context from Responses

If your server returns trace context in response headers:

```kotlin
val responseHeaders = response.headers.toMap()
val traceContext = TraceContextPropagator.extractContext(responseHeaders)
```

## W3C Trace Context Format

The implementation follows the W3C Trace Context specification:

**traceparent Format:**
```
version-traceId-spanId-traceFlags
00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

- **Version**: 00 (current spec version)
- **TraceId**: 32-character hex string (16 bytes)
- **SpanId**: 16-character hex string (8 bytes)
- **TraceFlags**: 2-character hex string
  - `01`: Sampled (traced)
  - `00`: Not sampled (not traced)

**tracestate Format:**
```
vendorname=opaquevalue, vendorname2=opaquevalue2
```

## Integration with Backend

### Server-side Configuration

1. **Extract traceparent and tracestate headers** from incoming requests
2. **Create child spans** for processing
3. **Inject headers into downstream calls** to other services
4. **Propagate context** through your observability platform (Jaeger, Datadog, etc.)

### Example Spring Boot Server

```kotlin
@GetMapping("/api/ui/landing")
fun getLanding(
    @RequestHeader(value = "traceparent", required = false) traceparent: String?,
    @RequestHeader(value = "tracestate", required = false) tracestate: String?
): ResponseEntity<UiComponent> {
    // Extract context
    val context = TracingUtil.extractContext(traceparent, tracestate)
    
    // Set context for current span
    TracingUtil.setContext(context)
    
    // Your business logic
    val ui = buildUi()
    
    // Create response with trace headers
    val response = ResponseEntity.ok(ui)
    response.headers["traceparent"] = context.toTraceparent()
    
    return response
}
```

## Single Pane of Glass Setup

To create a unified observability dashboard:

### 1. Collect Traces

Configure your backend to send traces to your observability platform:

```yaml
# Example: Jaeger exporter configuration
otel:
  exporter:
    jaeger:
      endpoint: http://localhost:14268/api/traces
```

### 2. Android Instrumentation (Optional)

Add OpenTelemetry SDK for Android:

```gradle
dependencies {
    implementation("io.opentelemetry.android:instrumentation-api:0.1.0")
}
```

### 3. Dashboard Configuration

In Jaeger, Datadog, or your preferred tool:
- Create searches by `trace_id` to correlate all spans
- Filter by `service.name` to separate client/server/backend spans
- Set up alerts on high latency or error rates

### 4. View Full Trace

Example trace path:
```
Mobile App (TraceID: xyz) 
  → GET /api/ui/landing (SpanID: a)
    → Server Processing (SpanID: b, parent: a)
      → Database Query (SpanID: c, parent: b)
      → Cache Lookup (SpanID: d, parent: b)
```

## Debugging

Enable trace output in the client:

```
// Logs in the format:
🔍 [TRACE] Traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
🔍 [TRACE] Request completed - TraceID: 4bf92f3577b34da6a3ce929d0e0e4736, SpanID: 00f067aa0ba902b7, Duration: 250ms
```

## Testing

The tracing functionality can be tested without a backend:

```kotlin
@Test
fun testTraceContextPropagation() {
    val context = TraceContext.create()
    val headers = mutableMapOf<String, String>()
    
    TraceContextPropagator.injectContext(context, headers)
    
    assertEquals(context.toTraceparent(), headers["traceparent"])
    assertEquals(context.traceState, headers["tracestate"])
}

@Test
fun testTraceContextExtraction() {
    val headers = mapOf(
        "traceparent" to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    )
    
    val context = TraceContextPropagator.extractContext(headers)
    
    assertNotNull(context)
    assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context?.traceId)
    assertEquals("00f067aa0ba902b7", context?.spanId)
}
```

## Architecture Diagram

```
┌─────────────────────────────────────┐
│      Mobile App (SDUI Client)       │
│                                     │
│  ┌──────────────────────────────┐   │
│  │    LandingViewModel           │   │
│  │  - Manages Trace Context      │   │
│  │  - Initializes TraceID        │   │
│  └──────────────────────────────┘   │
│                ↓                     │
│  ┌──────────────────────────────┐   │
│  │    UiRepository              │   │
│  │  - Sets Trace Context        │   │
│  │  - Makes HTTP Requests       │   │
│  └──────────────────────────────┘   │
│                ↓                     │
│  ┌──────────────────────────────┐   │
│  │    HttpClientFactory          │   │
│  │  - TracingPlugin installed    │   │
│  │  - Injects Headers            │   │
│  └──────────────────────────────┘   │
│                ↓                     │
│  ┌──────────────────────────────┐   │
│  │  Ktor HTTP Client             │   │
│  │  Headers:                      │   │
│  │  - traceparent: 00-xxxx-xxxx-01│  │
│  │  - tracestate: <optional>      │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
           ↓
        Network
           ↓
┌─────────────────────────────────────┐
│      Backend Server                 │
│  - Extracts traceparent header      │
│  - Creates server span              │
│  - Injects into downstream calls    │
│  - Sends traces to observability    │
└─────────────────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│   Observability Platform            │
│   (Jaeger, Datadog, Honeycomb, etc.)│
│                                     │
│   Single Pane of Glass Dashboard    │
│   - View end-to-end traces          │
│   - Monitor latencies               │
│   - Identify bottlenecks            │
│   - Correlation analysis            │
└─────────────────────────────────────┘
```

## Next Steps

1. **Server Integration**: Implement trace context extraction and injection on the backend
2. **Backend Instrumentation**: Use OpenTelemetry SDK to send traces
3. **Observability Setup**: Deploy Jaeger, Datadog, or similar for trace collection
4. **Dashboard Creation**: Build queries to view end-to-end traces
5. **Alert Configuration**: Set up alerts for anomalies in trace data