# Distributed Tracing & Error Monitoring - Implementation Summary

## Overview

A complete distributed tracing and error monitoring system has been implemented using OpenTelemetry (OTEL) and Jaeger. The system automatically tracks HTTP requests, captures errors, and exports traces for end-to-end observability.

**Key Achievement**: Error spans are ALWAYS exported to Jaeger for visibility, even when sampling is disabled.

## What Was Implemented

### 1. Core Tracing Architecture

#### Span Model (`tracing/Span.kt`)
- **Data Structure**: Represents operations with traceId, spanId, parentSpanId, name, status, attributes
- **Lifecycle**: startTime, endTime, status (UNSET/OK/ERROR)
- **Sampling**: traceFlags ("01" = sampled, "00" = not sampled)
- **Factory**: `Span.create()` respects SamplingConfig for trace flags

#### SpanContextHolder (`tracing/Span.kt`)
- Thread-safe storage for current span context
- Used to correlate errors with their HTTP spans
- Platform-agnostic (works across Android, iOS, Web)

#### TracingProvider (`tracing/TracingProvider.kt`)
- **Span Lifecycle Management**
  - `startSpan()` - Create child spans
  - `endSpan()` - Close span and export if sampled OR has ERROR status
  - `recordError()` - Add error details (type, message) to span
- **Key Logic**: Error spans bypass sampling - always exported
- **Export Trigger**: `exportIfSampled()` checks `isSampled || status == ERROR`

#### TracingPlugin (`tracing/TracingPlugin.kt`)
- Ktor HTTP client plugin for automatic request tracking
- `onRequest`: Creates child span, injects W3C traceparent header
- `onResponse`: Sets span status based on HTTP code (>= 400 = ERROR)
- Automatic header injection: `traceparent` and `tracestate`

#### JaegerSpanExporter (`tracing/SpanExporter.kt`)
- OTLP (OpenTelemetry Protocol) format exporter
- Sends spans to Jaeger collector at configurable endpoint
- **Critical Filter**: Exports if `traceFlags == "01" || status == SpanStatus.ERROR`
- Batches and retries on failure

#### AppInitializer (`AppInitializer.kt`)
- **Initialization Flow**:
  1. Set SamplingConfig (environment + isQaUser)
  2. Initialize Jaeger exporter
  3. Create root span (respects sampling config)
  4. Store in SpanContextHolder
- **One-time Setup**: Called once at app startup before any ViewModels

### 2. Error Tracking Pipeline

#### HTTP Error Detection
```
HTTP Request (status >= 400)
    ↓
TracingPlugin.onResponse()
    ├─ Set status = ERROR
    └─ Call endSpan(span, ERROR)
    
TracingProvider.endSpan()
    ├─ exportIfSampled() checks: isSampled || status == ERROR
    └─ Always exports (because ERROR)
    
JaegerSpanExporter
    ├─ Filter: traceFlags == "01" || status == ERROR
    └─ Always exports (because ERROR)
```

#### Error Recording
```
UiRepository.fetchLanding() throws exception
    ↓
LandingViewModel.load() catches error
    ├─ recordError(parentSpan, exception)
    └─ Span status already set to ERROR from HTTP response
    
recordError() adds attributes:
    ├─ error.type: Exception class name
    └─ error.message: Exception message
```

### 3. Sampling Configuration

#### SamplingConfig (`tracing/SamplingConfig.kt`)
```kotlin
data class SamplingConfig(
    val environment: Environment,
    val isQaUser: Boolean = false
)

enum class Environment {
    DEVELOPMENT,  // 100% sampling
    STAGING,      // 20% sampling
    QA,          // 100% sampling
    PRODUCTION   // 1% sampling
}
```

#### Sampling Behavior
- **Root span**: Respects environment sampling rate
- **HTTP request spans**: Inherit parent's sampling decision
- **Error spans**: ALWAYS exported (forced sampling)
- **W3C Compliance**: Trace context headers sent even for non-sampled traces

### 4. Integration Points

#### App Startup (MainActivity/MainViewController)
```kotlin
// Before creating any ViewModels
AppInitializer.initializeApp()

// Then create ViewModel - it will have access to root span
val viewModel = remember { LandingViewModel() }
```

#### HTTP Client (`HttpClientFactory.kt`)
- TracingPlugin installed automatically
- All requests include traceparent/tracestate headers
- Span created/ended for each request

#### Error Handling (`BaseViewModel.kt`)
```kotlin
protected fun recordError(error: Throwable) {
    // Uses global span from SpanContextHolder as fallback
    val span = _span.value ?: SpanContextHolder.current()
    if (span != null) {
        TracingProvider.recordError(span, error)
    }
}
```

### 5. Response Status Handling

#### HTTP Status → Span Status Mapping
```
HTTP Status < 400 → SpanStatus.OK (green in Jaeger)
HTTP Status >= 400 → SpanStatus.ERROR (red in Jaeger)

Error Attributes Added:
- error.type: Exception class name
- error.message: Full error message
- http.status_code: HTTP response code
- http.method: HTTP method (GET, POST, etc.)
- http.url: Full request URL
- device.id: Unique device identifier
- device.os: Platform (android/ios/web)
```

### 6. Double Fetch Prevention

#### Problem Solved
Original issue: `fetchLanding()` was called twice on app load
- Once in ViewModel.init
- Again when sampling was configured

#### Solution
- Move sampling initialization to app startup (`AppInitializer`)
- Initialize BEFORE creating any ViewModels
- Remove reload trigger from sampling callbacks
- Single fetch with trace context available

## Architecture Diagram

```
App Startup
    ↓
AppInitializer.initializeApp()
    ├─ Set SamplingConfig(DEVELOPMENT)
    ├─ Create Jaeger exporter
    ├─ Create root span (traceFlags based on env)
    └─ Store in SpanContextHolder
    ↓
MainActivity/MainViewController
    ├─ Create LandingViewModel
    ├─ ViewModel.init calls load()
    └─ load() → fetchLanding() [SINGLE FETCH]
    ↓
HTTP Request (TracingPlugin)
    ├─ Create child span
    ├─ Inject traceparent header
    ├─ Make request to server
    └─ Receive response
    ↓
Response Handler
    ├─ Status < 400 → SpanStatus.OK
    ├─ Status >= 400 → SpanStatus.ERROR
    └─ Export span (always if ERROR)
    ↓
Jaeger
    ├─ Receive OTLP payload
    ├─ Store trace with all spans
    └─ Display in UI
```

## Key Files

| File | Purpose |
|------|---------|
| `tracing/Span.kt` | Span model and SpanContextHolder |
| `tracing/TracingProvider.kt` | Span lifecycle management |
| `tracing/TracingPlugin.kt` | HTTP client integration |
| `tracing/SpanExporter.kt` | Jaeger OTLP exporter |
| `AppInitializer.kt` | App-level initialization |
| `PlatformConfig.kt` | Jaeger endpoint configuration |
| `viewmodel/BaseViewModel.kt` | Error recording integration |
| `viewmodel/LandingViewModel.kt` | Error catching and reporting |
| `data/UiRepository.kt` | HTTP response handling |
| `network/HttpClientFactory.kt` | HTTP client setup |

## Features Implemented

### ✅ Automatic HTTP Tracing
- Every request creates a span
- Headers injected automatically
- Span closed on response

### ✅ Error Detection & Tracking
- HTTP errors (>= 400) detected automatically
- Error details captured (type, message)
- Error spans marked with ERROR status

### ✅ Forced Sampling for Errors
- Error spans ALWAYS exported (regardless of sampling config)
- Ensures error visibility in Jaeger even in production (1% sampling)

### ✅ W3C Trace Context
- Standard traceparent/tracestate headers
- Compatible with all observability platforms
- Enables backend integration

### ✅ Environment-Based Sampling
- Development: 100%
- Staging: 20%
- QA: 100%
- Production: 1%

### ✅ Zero Configuration
- Works out of the box
- Sensible defaults per environment
- Single initialization call

### ✅ Double Fetch Prevention
- Sampling initialized before ViewModels
- Single fetch on app load
- Trace context available immediately

## Trace Flow Example

```
[Client App]
  Span: "GET /api/ui/landing" (traceId=abc123, spanId=def456)
    Status: ERROR (HTTP 500)
    Attributes:
      - http.status_code: 500
      - error.type: Exception
      - error.message: HTTP 500: Internal Server Error...
    ↓ [W3C Traceparent Header Sent]
    
[Server]
  Extract: traceparent: 00-abc123-def456-01
  Create child span with same traceId
  Process request
  Return response with traceparent
    ↓
    
[Jaeger]
  Receive OTLP payload
  Display trace:
    - Mobile app span (red - error)
    - Server spans (children of app span)
    - Full timeline and latency
```

## Jaeger Integration

### Setup
```bash
docker run -p 16686:16686 -p 4318:4318 jaegertracing/all-in-one
```

### View Traces
1. Open `http://localhost:16686`
2. Select service: `sdui-mobile-client`
3. View errors (red spans) and their details

### Endpoint Configuration
- **Android emulator**: `http://10.0.2.2:4318/v1/traces`
- **iOS simulator**: `http://localhost:4318/v1/traces`
- **Web**: `http://localhost:4318/v1/traces`

## Testing

### Manual Testing
1. Start Jaeger: `docker-compose up jaeger`
2. Run app
3. Make request that returns 500
4. Check Jaeger UI for red error span

### Expected Output
```
🔍 [TRACE] Sampling configured - Environment: DEVELOPMENT, IsQaUser: false
🔍 [TRACE] Initial Span set - TraceID: abc123..., Sampled: true

🔍 [SPAN] Started: GET http://... (spanId)
🔍 [SPAN] Ended: GET ... - 755ms - Status: ERROR
📤 [JAEGER] Sending payload to http://10.0.2.2:4318/v1/traces
✅ [JAEGER] Exported 1/1 sampled spans
```

## Performance Impact

- **Minimal Overhead**: Span creation/closing is lightweight
- **Async Export**: OTLP export in background coroutine
- **Memory**: Spans kept until exported, then cleaned up
- **Sampling**: Reduces load on Jaeger in production (1%)

## Documentation

- **CLAUDE.md**: Quick start and basic setup
- **TRACING.md**: Complete technical documentation
- Inline comments explain key decisions

## Benefits

✅ **Full Observability**: See all HTTP requests and errors  
✅ **Error Visibility**: Error spans always exported to Jaeger  
✅ **Zero Config**: Works out of the box  
✅ **Standards Compliant**: W3C Trace Context  
✅ **Multi-Platform**: Works on Android, iOS, Web  
✅ **Backend Integration**: Ready for server-side traces  

## Limitations & Future Work

- Current: Client-only tracing (mobile app)
- Future: Server-side span creation with same traceId
- Future: Cross-service correlation
- Future: Custom metrics and attributes

## Conclusion

A production-ready distributed tracing system with:
- Automatic HTTP request tracking
- Error detection and monitoring
- Forced sampling for errors (always exported)
- W3C standard trace context propagation
- Integration with Jaeger for visualization
- Zero configuration required
