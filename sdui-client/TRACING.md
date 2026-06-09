# Distributed Tracing & Error Tracking

This document describes the distributed tracing and error monitoring implementation for the SDUI client using OpenTelemetry (OTEL) and Jaeger.

## Overview

The SDUI client automatically tracks all HTTP requests and errors using W3C Trace Context propagation. Error spans are always exported to Jaeger for visibility, even when sampling is disabled.

## Architecture

### Key Components

#### 1. **Span** (`commonMain/tracing/Span.kt`)
Represents a single operation in the trace:

```kotlin
data class Span(
    val traceId: String,           // Unique trace identifier
    val spanId: String,            // Unique span identifier
    val parentSpanId: String? = null,
    val traceFlags: String = "01", // "01" = sampled, "00" = not sampled
    val traceState: String = "",
    val name: String = "span",
    val startTime: Long,
    var endTime: Long? = null,
    var status: SpanStatus = SpanStatus.UNSET,
    var attributes: Map<String, String> = emptyMap()
)

enum class SpanStatus {
    UNSET, OK, ERROR
}
```

#### 2. **SpanContextHolder** (`commonMain/tracing/Span.kt`)
Thread-safe holder for the current span context:

```kotlin
SpanContextHolder.current()   // Get current span
SpanContextHolder.set(span)   // Set current span
SpanContextHolder.clear()     // Clear context
```

#### 3. **TracingProvider** (`commonMain/tracing/TracingProvider.kt`)
Manages span lifecycle and export:

```kotlin
TracingProvider.startSpan(name, parentSpan)     // Create child span
TracingProvider.endSpan(span, status)           // End and export span
TracingProvider.recordError(span, error)        // Record error on span
```

**Error Handling:**
- When `recordError()` is called:
  - Error attributes (type, message) are added to the span
  - Span status is set to ERROR
  - Span is exported immediately if not already exported

#### 4. **AppInitializer** (`commonMain/AppInitializer.kt`)
Application-level tracing setup:

```kotlin
AppInitializer.initializeApp()  // Call once at app startup
```

**Setup Flow:**
1. Creates initial root span with unique traceId
2. Configures SamplingConfig for the environment
3. Initializes JaegerSpanExporter
4. Sets root span in SpanContextHolder

#### 5. **TracingPlugin** (`commonMain/tracing/TracingPlugin.kt`)
Ktor HTTP client plugin for automatic span tracking:

```kotlin
onRequest { request ->
    // Creates child span for HTTP request
    // Injects traceparent and tracestate headers
}

onResponse { response ->
    // Ends span with status based on HTTP code
    // Status = ERROR if HTTP >= 400
    // Status = OK if HTTP < 400
}
```

#### 6. **JaegerSpanExporter** (`commonMain/tracing/SpanExporter.kt`)
Exports spans to Jaeger using OTLP protocol:

```kotlin
// Filter logic:
val shouldExport = span.traceFlags == "01" || span.status == SpanStatus.ERROR
// Always export if sampled OR if error
```

## Trace Flow

```
App Startup
    ↓
AppInitializer.initializeApp()
    ├─ Set SamplingConfig
    └─ Initialize Jaeger exporter
    
HTTP Request
    ↓
TracingPlugin.onRequest()
    ├─ Create child span
    ├─ Inject traceparent header
    └─ Store in request attributes
    
HTTP Response
    ↓
TracingPlugin.onResponse()
    ├─ Add http.status_code attribute
    ├─ Set status: ERROR (if >= 400) or OK
    ├─ Call TracingProvider.endSpan()
    └─ Export to Jaeger
    
If Error Occurs
    ↓
recordError(span, exception)
    ├─ Add error.type attribute
    ├─ Add error.message attribute
    ├─ Set status = ERROR
    └─ (Already exported in endSpan)
```

## Sampling Configuration

### Environment-Based Sampling

The initial root span is created with `traceFlags="01"` (always sampled). This ensures:
- Root span is always visible in Jaeger..
- All child spans inherit sampling decision
- Error spans are ALWAYS exported (regardless of sampling)

### SamplingConfig

```kotlin
data class SamplingConfig(
    val environment: Environment,
    val isQaUser: Boolean = false
)

enum class Environment {
    DEVELOPMENT,    // 100% sampling (local dev)
    STAGING,        // 20% sampling
    QA,            // 100% sampling
    PRODUCTION     // 1% sampling
}
```

## HTTP Error Handling

### When HTTP Status >= 400

1. **TracingPlugin.onResponse()**
   ```kotlin
   val status = if (response.status.value >= 400) 
       SpanStatus.ERROR 
   else 
       SpanStatus.OK
   TracingProvider.endSpan(span, status)
   ```

2. **exportIfSampled()**
   ```kotlin
   val shouldExport = span.isSampled || span.status == SpanStatus.ERROR
   // Always export if ERROR, even if not sampled
   ```

3. **JaegerSpanExporter**
   ```kotlin
   val spansToExport = spans.filter { 
       it.traceFlags == "01" || it.status == SpanStatus.ERROR 
   }
   ```

4. **Result in Jaeger**
   - HTTP span shows as RED (error status)
   - With all error details in attributes

### Error Attributes

When an error occurs:
```
http.method: "GET"
http.url: "http://10.0.2.2:8080/api/ui/landing"
http.status_code: "500"
error.type: "Exception"
error.message: "HTTP 500: Internal Server Error..."
device.id: "fffee99e-1318-4196-b6c2-28d385908d75"
device.os: "android"
```

## Trace Context Propagation

### W3C Traceparent Header

All HTTP requests include:
```
traceparent: 00-<traceId>-<spanId>-<traceFlags>

Example:
traceparent: 00-d66ac09547b268bb58b0fb8090643fa3-d0b2e7e2f9cf8b2d-01
```

- **Version**: 00 (W3C spec version)
- **TraceID**: 32-character hex (16 bytes)
- **SpanID**: 16-character hex (8 bytes)
- **TraceFlags**: 01 (sampled) or 00 (not sampled)

### Backend Integration

Servers can extract and process trace context:

```java
@GetMapping("/api/ui/landing")
public ResponseEntity<?> getLanding(
    @RequestHeader(value = "traceparent", required = false) String traceparent,
    @RequestHeader(value = "tracestate", required = false) String tracestate
) {
    // Extract trace context from header
    // Create server-side spans
    // Inject into downstream calls
    // Return response
}
```

## Jaeger Setup

### Docker Compose

```yaml
version: '3'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI port
      - "4318:4318"    # OTLP receiver (http)
      - "4317:4317"    # OTLP receiver (grpc)
    environment:
      - COLLECTOR_OTLP_ENABLED=true
```

### Access Jaeger UI

- **Local**: `http://localhost:16686`
- **Service Name**: `sdui-mobile-client`

### View Traces

1. Open Jaeger UI
2. Select service: `sdui-mobile-client`
3. Search by:
   - **Trace ID** (from logs: `[TRACE] Traceparent: 00-<traceId>-...`)
   - **Service name** → find by operation
   - **Tags** → filter by `error` status

## Logging Output

### Span Lifecycle Logs

```
🔍 [TRACE] Sampling configured - Environment: DEVELOPMENT, IsQaUser: false
🔍 [TRACE] Initial Span set - TraceID: abc123...
🔍 [TRACE] Traceparent: 00-abc123-def456-01

🔍 [SPAN] Started: GET http://10.0.2.2:8080/api/ui/landing (spanId)
🔍 [HTTP] Request started: GET http://10.0.2.2:8080/api/ui/landing
🔍 [SPAN] Ended: GET ... - 755ms - Status: ERROR
🔍 [HTTP] Response: 500

🔍 [ERROR] Exception: HTTP 500: ...
📤 [JAEGER] Sending payload to http://10.0.2.2:4318/v1/traces
✅ [JAEGER] Exported 1/1 sampled spans
```

## Key Files

| File | Purpose |
|------|---------|
| `commonMain/tracing/Span.kt` | Span data model and SpanContextHolder |
| `commonMain/tracing/TracingProvider.kt` | Span lifecycle management |
| `commonMain/tracing/TracingPlugin.kt` | Ktor HTTP client plugin |
| `commonMain/tracing/SpanExporter.kt` | Jaeger OTLP exporter |
| `commonMain/AppInitializer.kt` | App-level initialization |
| `commonMain/PlatformConfig.kt` | Platform-specific configuration |

## Performance Considerations

1. **Sampling** - Reduces load on Jaeger:
   - Dev: 100% (for debugging)
   - Prod: 1% (cost control)
   - Error spans: Always (importance-based)

2. **Async Export** - Spans exported in background:
   - Non-blocking HTTP posts
   - Batched when possible
   - Failures logged but don't crash app

3. **Memory** - Spans kept in memory until exported:
   - Small overhead per request
   - Cleaned up after export

## Debugging Tips

### Missing Traces in Jaeger

1. **Check Jaeger is running**
   ```bash
   curl http://localhost:16686/api/services
   ```

2. **Verify endpoint in logs**
   - Should see: `📤 [JAEGER] Sending payload to...`
   - Check endpoint matches Jaeger collector

3. **Check service name**
   - Should be: `sdui-mobile-client`
   - Filter by this in Jaeger UI

4. **Error spans not visible**
   - Check span status is ERROR
   - Ensure error attributes are set
   - Verify export log: `✅ [JAEGER] Exported`

### Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| No traces in Jaeger | Jaeger not running | Start Jaeger container |
| Spans not exported | Wrong endpoint | Verify `PlatformConfig.jaegerEndpoint` |
| HTTP 500 shows as green | Status not set to ERROR | Check TracingPlugin.onResponse logic |
| Only errors exported | Non-sampled requests filtered | Check sampling config |

## Testing

### Manual Test

1. Run app with Jaeger container
2. Make a request that returns 500
3. Check logs for export message
4. Open Jaeger UI → search service
5. Filter by error status or trace ID

### Automated Test (Unit)

```kotlin
@Test
fun testErrorSpanExport() {
    val span = Span.create(traceFlags = "01")
    val error = Exception("Test error")
    
    TracingProvider.recordError(span, error)
    
    assertEquals(SpanStatus.ERROR, span.status)
    assertTrue(span.attributes.containsKey("error.type"))
    assertTrue(span.attributes.containsKey("error.message"))
}
```

## Next Steps

1. **Set up Jaeger locally** - See Docker Compose above
2. **Run the app** - Traces automatically exported
3. **View in Jaeger UI** - Search by service or trace ID
4. **Integrate with backend** - Extract traceparent headers
5. **Setup alerts** - Monitor error rates in Jaeger
