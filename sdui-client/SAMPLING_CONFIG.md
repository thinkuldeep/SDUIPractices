# Sampling Configuration Guide

This guide explains how to configure trace sampling based on your deployment environment.

## Quick Start

### Default Behavior (Development)
```kotlin
// In MainActivity or app entry point
AppInitializer.initializeApp()  // Uses DEVELOPMENT environment, 100% sampling
```

**Default Sampling Rates:**
- **Development**: 100% (local debugging)
- **Staging**: 20% (pre-production validation)
- **QA**: 100% (complete test visibility)
- **Production**: 1% (cost-effective)

### For Other Environments
```kotlin
// Staging
AppInitializer.initializeSampling(Environment.STAGING)

// QA
AppInitializer.initializeSampling(Environment.QA)

// Production
AppInitializer.initializeSampling(Environment.PRODUCTION)
```

**Important**: Call `AppInitializer` at app startup, BEFORE creating any ViewModels.

## Environment-Specific Configuration

### Production Environment (1% Sampling)

**Goal**: Minimize costs while maintaining error visibility.

```kotlin
// MainActivity.kt
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize tracing for production
        AppInitializer.initializeSampling(Environment.PRODUCTION)

        setContent {
            val viewModel = remember { LandingViewModel() }
            // ... rest of UI
        }
    }
}
```

**Result**:
- 1% of requests traced (traceFlags="01")
- 99% non-sampled (traceFlags="00")
- ALL errors exported (forced sampling)
- Cost: ~$0.0001/month for 1M requests/day

**Log Output**:
```
🔍 [TRACE] Sampling configured - Environment: PRODUCTION, IsQaUser: false
🔍 [TRACE] Initial Span set - TraceID: 4bf92f3577b34da6a, Sampled: false
```

### Staging Environment (20% Sampling)

**Goal**: Pre-production validation with good coverage.

```kotlin
AppInitializer.initializeSampling(Environment.STAGING)
```

**Result**:
- 20% of requests traced (1 in 5)
- Good coverage for testing
- Cost: ~$0.006/month for 1M requests/day

### QA Environment (100% Sampling)

**Goal**: Full visibility for regression testing.

```kotlin
AppInitializer.initializeSampling(Environment.QA)
```

**Result**:
- 100% of requests traced
- Complete visibility
- Cost: ~$0.03/month for 1M requests/day

### Development Environment (100% Sampling)

**Goal**: Maximum visibility for local debugging (default).

```kotlin
AppInitializer.initializeApp()  // Uses DEVELOPMENT
```

**Result**:
- 100% sampling
- See every request in Jaeger
- Unlimited cost (local only)

## Error Sampling (Special Behavior)

### Key Feature: Errors Always Exported

Regardless of environment, ALL error spans are exported to Jaeger:

```
Production: 1% sampling
  ├─ Successful request (200) → 99% chance NOT exported
  ├─ Successful request (200) → 1% chance exported
  ├─ Error request (500) → ALWAYS exported (forced sampling!)
  └─ Error request (500) → ALWAYS exported
```

This ensures complete error visibility even in production.

### HTTP Status → Sampling Decision

```
HTTP Status < 400 → Respects environment sampling
  Development: Always sampled
  Staging: 20% sampled
  Production: 1% sampled

HTTP Status >= 400 → ALWAYS sampled (forced)
  Development: Always sampled
  Staging: Always sampled  
  Production: Always sampled (override 1% rule!)
```

## Advanced Configuration

### BuildConfig-Based Environment

```kotlin
fun getSamplingEnvironment(): Environment {
    return when {
        BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "prod" ->
            Environment.PRODUCTION
        BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "staging" ->
            Environment.STAGING
        BuildConfig.BUILD_TYPE == "debug" ->
            Environment.DEVELOPMENT
        else -> Environment.QA
    }
}

// Usage
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppInitializer.initializeSampling(getSamplingEnvironment())
    // ...
}
```

### Remote Configuration

```kotlin
suspend fun configureSamplingFromRemote() {
    val remoteConfig = fetchRemoteConfig()  // Firebase, etc.
    
    val environment = when (remoteConfig.getString("environment")) {
        "prod" -> Environment.PRODUCTION
        "staging" -> Environment.STAGING
        "qa" -> Environment.QA
        else -> Environment.DEVELOPMENT
    }
    
    AppInitializer.initializeSampling(environment)
}
```

## Trace Propagation

### W3C Trace Context Header

Every HTTP request includes:

```http
traceparent: 00-<traceId>-<spanId>-<traceFlags>
tracestate: device-id=<uuid>,device-os=<platform>
```

**Example (Production)**:
```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00
             ├─ version (00)
             ├─ traceId (32 chars)
             ├─ spanId (16 chars)
             └─ traceFlags (00=not sampled, 01=sampled)
```

**Key Point**: ALL requests send traceparent headers, even non-sampled ones. Backend can:
- Correlate requests across services
- Make independent sampling decisions
- Implement tail sampling for errors

## Span Lifecycle

### Sampling Decision

```
App Startup
    ↓
AppInitializer.initializeSampling(environment)
    ├─ Set SamplingConfig
    ├─ Initialize Jaeger exporter
    └─ Create root span (respects environment sampling)
    
HTTP Request
    ├─ Create child span
    ├─ Inherit parent's sampling decision
    └─ If error (>= 400): Force traceFlags="01"
    
Export Decision
    ├─ If sampled (traceFlags="01"): Export
    ├─ If error status: Export (always)
    └─ Otherwise: Skip export
```

## Monitoring

### Check Sampling Configuration

```kotlin
// During app initialization
val span = SpanContextHolder.current()
println("Root Span Sampled: ${span?.isSampled}")

// Logs:
// 🔍 [TRACE] Initial Span set - TraceID: 4bf92f3..., Sampled: true
```

### Log Analysis

```bash
# Count sampled vs non-sampled spans in production
grep "Sampled: true" app.log | wc -l   # Should be ~1%
grep "Sampled: false" app.log | wc -l  # Should be ~99%
```

### Metrics

Track actual sampling in production:

```
Sampled requests (traceFlags=01):  ~1% ✓
Non-sampled requests (traceFlags=00): ~99% ✓
Error requests exported: 100% ✓
```

## Cost Analysis

### Monthly Cost Estimates

For **1 Million Requests/Day** (30M/month):

| Environment | Rate | Traces/Day | Storage/Mo | Cost/Mo |
|---|---|---|---|---|
| Development | 100% | 1,000,000 | 1 GB | $0.01 |
| Staging | 20% | 200,000 | 200 MB | $0.002 |
| QA | 100% | 1,000,000 | 1 GB | $0.01 |
| Production | 1% + Errors | ~10,500 | ~11 MB | $0.0001 |

**Key**: Production with forced error sampling is extremely cost-effective.

### Cost Reduction

Increase production traffic from 1M to 10M requests/day:

| Environment | Requests/Day | Traces/Day | Cost/Mo |
|---|---|---|---|
| 1% sampling | 10,000,000 | 100,000 | $0.001 |
| + All errors | 10,000,000 | ~105,000 | $0.001 |

Even at 10× traffic, production costs ~$0.001/month!

## Backend Integration

### Extract Trace Context

Server can extract and use client's sampling decision:

```java
@GetMapping("/api/ui/landing")
public ResponseEntity<?> getLanding(
    @RequestHeader(value = "traceparent", required = false) String traceparent
) {
    // Extract trace context
    TraceContext context = traceContextPropagator.extractContext(traceparent);
    
    // Use client's sampling decision
    boolean shouldSample = context != null && context.isSampled();
    
    // Or make independent decision (tail sampling)
    boolean hasError = /* check something */;
    if (hasError) {
        shouldSample = true;  // Always sample errors
    }
    
    // Create server span with decision
    Span serverSpan = tracer.spanBuilder("GET /api/ui/landing")
        .setSampled(shouldSample)
        .startSpan();
    
    return serverSpan.makeCurrent().wrap(() -> {
        // Process request
        return ResponseEntity.ok(buildLandingPage());
    });
}
```

### Tail Sampling

Backend implements tail sampling to capture important requests:

```java
public boolean shouldSampleBasedOnOutcome(RequestContext context) {
    return context.getHttpStatus() >= 500 ||      // Server errors
           context.getDurationMs() > 5000 ||      // Slow requests
           context.hasException() ||               // Any exception
           context.isSampled();                   // Client's decision
}
```

## Best Practices

✅ **Initialize Early**
- Call `AppInitializer` at app startup
- Before creating any ViewModels
- So trace context is available immediately

✅ **Use Environment Detection**
- Detect from BuildConfig/Flavor
- Or from remote config
- Not hardcoded in app

✅ **Monitor Errors**
- All errors exported (forced sampling)
- Check Jaeger for error visibility
- Set up alerts on error spikes

✅ **Cost Management**
- Start with 1% sampling in production
- Increase if budget allows
- Use Jaeger storage metrics

✅ **Backend Cooperation**
- Extract traceparent headers
- Propagate to downstream services
- Implement tail sampling for errors

## Troubleshooting

### No traces in Jaeger

**Check**: Is sampling configured?
```
🔍 [TRACE] Sampling configured - Environment: PRODUCTION
```

**Check**: Is Jaeger running?
```bash
curl http://localhost:4318/v1/traces
# Should return (not error)
```

### Traces not showing in production (1% sampling)

**Note**: This is expected!
- 99% of successful requests not traced
- Only errors always traced
- Check Jaeger for error filters

### Too many traces (high cost)

**Solution**: Reduce sampling rate
```kotlin
AppInitializer.initializeSampling(Environment.PRODUCTION)  // 1%
```

Or use staging for validation:
```kotlin
AppInitializer.initializeSampling(Environment.STAGING)  // 20%
```

### Wrong environment selected

**Verify**: BuildConfig is correct
```kotlin
println(BuildConfig.FLAVOR)      // Should match environment
println(BuildConfig.BUILD_TYPE)  // release vs debug
```

## Summary

✅ **One-Time Setup**
- Call `AppInitializer.initializeSampling(environment)` at app startup
- Before creating any ViewModels

✅ **Automatic Sampling**
- Environment determines sampling rate
- Error spans always exported (forced)
- W3C headers sent to all requests

✅ **Cost Effective**
- Production: 1% sampling = minimal cost
- Errors: Always visible = full error tracking
- Backend: Can override decisions

✅ **Production Ready**
- Works out of the box
- Sensible defaults per environment
- Jaeger integration ready
