# OpenTelemetry Tracing - Practical Examples

This document provides practical examples of how to use the OTEL tracing implementation in the SDUI client.

## Basic Usage

### Example 1: Automatic Tracing (Default Behavior)

The simplest usage - just create a ViewModel and tracing happens automatically:

```kotlin
// In your Activity/ViewController
val viewModel = LandingViewModel()

// Logs:
// 🔥 ViewModel INIT
// 🔍 [TRACE] Traceparent: 00-<traceId>-<spanId>-01
// 🔍 [TRACE] Request completed - TraceID: <traceId>, SpanID: <spanId>, Duration: XXms
```

**What happens:**
- A new trace ID is generated automatically
- All HTTP requests include the `traceparent` and `tracestate` headers
- Trace context is stored in thread-local storage for the duration of the request

### Example 2: Passing Trace Context from Parent Application

If your app is launched from another application that sends trace context:

```kotlin
// In your entry point (Activity/ViewController)
val parentTraceparent = intent.getStringExtra("traceparent") // from parent app
val parentTracestate = intent.getStringExtra("tracestate") ?: ""

val viewModel = LandingViewModel()
viewModel.setTraceContext(
    traceparent = parentTraceparent ?: "00-" + generateNewTraceId() + "-" + generateNewSpanId() + "-01",
    tracestate = parentTracestate
)
```

**What happens:**
- The trace context from the parent app is used
- All subsequent requests will have the same trace ID as the parent
- This allows end-to-end tracing across multiple applications

### Example 3: Manual Trace Control

For advanced scenarios where you need more control:

```kotlin
val viewModel = LandingViewModel()

// Check current trace context
val currentContext = viewModel.getCurrentTraceContext()
println("Current Trace ID: ${currentContext?.traceId}")

// Create a custom trace context
val customContext = TraceContext.create(
    traceId = "4bf92f3577b34da6a3ce929d0e0e4736",
    spanId = "00f067aa0ba902b7",
    traceState = "dd=ssd:2"
)
viewModel.setTraceContext(customContext)
```

## Server-Side Integration Examples

### Example 1: Spring Boot Server

```kotlin
@RestController
@RequestMapping("/api/ui")
class UiController(
    private val tracingService: TracingService
) {
    @GetMapping("/landing")
    fun getLanding(
        @RequestHeader(value = "traceparent", required = false) traceparent: String?,
        @RequestHeader(value = "tracestate", required = false) tracestate: String?
    ): ResponseEntity<UiComponent> {
        // Extract trace context from request
        val traceContext = tracingService.extractTraceContext(traceparent, tracestate)
        
        // Set context for current span
        tracingService.setContext(traceContext)
        
        // Your business logic
        val ui = buildLandingPage()
        
        // Log the trace information
        logger.info("Landing page served - TraceID: ${traceContext.traceId}")
        
        return ResponseEntity.ok(ui)
    }
}
```

### Example 2: Ktor Server

```kotlin
fun Application.configureTracing() {
    install(Tracing) {
        headerToRead = "traceparent"
    }
    
    install(CallLogging) {
        filter { call ->
            call.request.path().startsWith("/api")
        }
    }
}

fun Route.uiRoutes(tracingService: TracingService) {
    get("/api/ui/landing") {
        val traceparent = call.request.header("traceparent")
        val tracestate = call.request.header("tracestate") ?: ""
        
        val context = tracingService.extractTraceContext(traceparent, tracestate)
        tracingService.setContext(context)
        
        val ui = buildLandingPage()
        call.respond(ui)
    }
}
```

### Example 3: Node.js/Express Server

```javascript
const express = require('express');
const { extractTraceContext, createChildSpan } = require('./tracing');

app.get('/api/ui/landing', (req, res) => {
    const traceparent = req.get('traceparent');
    const tracestate = req.get('tracestate') || '';
    
    // Extract parent trace context
    const parentContext = extractTraceContext(traceparent);
    
    // Create a child span for this server operation
    const childContext = createChildSpan(parentContext);
    
    console.log(`[TRACE] Processing landing - TraceID: ${childContext.traceId}, SpanID: ${childContext.spanId}`);
    
    const ui = buildLandingPage();
    
    // Send response with trace context (optional - for response tracing)
    res.set('traceparent', childContext.toTraceparent());
    res.json(ui);
});
```

## Database Query Integration

### Example: Propagate Trace Context to Database Operations

```kotlin
// Server-side
@Repository
class UiRepository(
    private val db: Database,
    private val tracingService: TracingService
) {
    fun getLandingContent(): UiComponent {
        val context = tracingService.currentContext()
        
        return db.transaction {
            // Pass trace ID to database query for correlation
            val span = tracingService.startSpan(
                "database.query",
                parentContext = context,
                attributes = mapOf(
                    "db.system" to "postgresql",
                    "db.operation" to "SELECT",
                    "db.name" to "content_db"
                )
            )
            
            try {
                val content = db.query(
                    "SELECT * FROM ui_components WHERE id = ? -- trace_id: ${context.traceId}",
                    listOf(1)
                )
                span.setAttribute("db.rows_returned", content.size)
                content
            } finally {
                span.end()
            }
        }
    }
}
```

## External Service Calls

### Example: Propagate Trace Context to Downstream Services

```kotlin
// Server calling another microservice
class ContentService(
    private val httpClient: HttpClient,
    private val tracingService: TracingService
) {
    suspend fun enrichContent(content: UiComponent): UiComponent {
        val context = tracingService.currentContext()
        
        val response = httpClient.get("https://content-service/api/enrich") {
            header("traceparent", context.toTraceparent())
            header("tracestate", context.traceState)
            header("X-Request-ID", context.spanId)
        }
        
        return response.body()
    }
}
```

## Observability Dashboard Setup

### Jaeger Configuration (docker-compose.yml)

```yaml
version: '3'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI port
      - "14268:14268"  # Collector port
    environment:
      - COLLECTOR_OTLP_ENABLED=true
      
  otel-collector:
    image: otel/opentelemetry-collector:latest
    ports:
      - "4317:4317"  # OTLP gRPC receiver
      - "4318:4318"  # OTLP HTTP receiver
    config:
      receivers:
        otlp:
          protocols:
            grpc:
              endpoint: 0.0.0.0:4317
            http:
              endpoint: 0.0.0.0:4318
      exporters:
        jaeger:
          endpoint: jaeger:14268
      service:
        pipelines:
          traces:
            receivers: [otlp]
            exporters: [jaeger]
```

### Sending Traces from Kotlin Server

Add dependency:
```gradle
implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.23.0")
```

Configure:
```kotlin
val jaegerExporter = JaegerGrpcSpanExporter.builder()
    .setEndpoint("http://localhost:14250")
    .build()

val tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
    .build()

OpenTelemetry.setGlobalTracerProvider(tracerProvider)

val tracer = OpenTelemetry.getTracer("sdui-server")
```

## Querying Traces in Jaeger UI

1. **Open Jaeger UI**: http://localhost:16686

2. **Find traces by Trace ID**:
   - Service: Select your service
   - Search by Trace ID: Paste the `traceId` from the client logs
   - View the complete trace visualization

3. **Example queries**:
   - Service: `sdui-client` | Operation: `fetchLanding`
   - Service: `sdui-server` | Operation: `GET /api/ui/landing`
   - Tags: `span.kind=client` | `span.kind=server`

4. **View latency breakdown**:
   - Client → Server network latency
   - Server processing time
   - Database query time
   - Total end-to-end latency

## Testing Trace Propagation

### Unit Test Example

```kotlin
@Test
fun testTraceContextPropagation() {
    val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    
    val viewModel = LandingViewModel()
    viewModel.setTraceContext(traceparent)
    
    val context = viewModel.getCurrentTraceContext()
    assertNotNull(context)
    assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId)
    assertEquals("00f067aa0ba902b7", context.spanId)
}
```

### Integration Test Example

```kotlin
@Test
fun testEndToEndTracing() {
    val testServer = TestServer()
    val testServer.setup()
    
    val traceparent = "00-testTrace123456789abcd-testSpan1234-01"
    
    val viewModel = LandingViewModel()
    viewModel.setTraceContext(traceparent)
    
    // Wait for request to complete
    val result = runBlocking {
        viewModel.uiState.first { it != null }
    }
    
    assertNotNull(result)
    
    // Verify server received the trace context
    val serverLog = testServer.getRequestHeaders()
    assertEquals(traceparent, serverLog["traceparent"])
}
```

## Debugging Trace Issues

### Common Issues and Solutions

**Issue: Trace ID is different in client and server logs**
- ✅ **Solution**: Ensure you're setting the trace context before the ViewModel makes the request
- Example: Call `viewModel.setTraceContext(traceparent)` before accessing `uiState`

**Issue: No traceparent header being sent**
- ✅ **Solution**: Check that TracingPlugin is installed in HttpClientFactory
- Verify with: Print the HTTP request headers before sending

**Issue: Trace context is being lost across requests**
- ✅ **Solution**: Use TraceContextHolder.set() to maintain context across async boundaries
- For long-lived operations, pass context explicitly to child operations

**Issue: Trace IDs keep changing during same session**
- ✅ **Solution**: Create trace context once in ViewModel init, don't create new contexts for each request
- Use `createChildContext()` to create child spans with the same trace ID

## Next Steps

1. **Deploy Jaeger**: Set up the observability platform
2. **Integrate Server**: Modify your backend to extract and propagate trace context
3. **Configure Exporters**: Send traces to Jaeger or your preferred tool
4. **Create Dashboards**: Build custom queries for your use cases
5. **Set Alerts**: Alert on high latency or errors in trace data