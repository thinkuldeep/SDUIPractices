# Sampling Configuration Guide

This guide explains how to configure trace sampling based on your deployment environment and user types.

## Quick Start

### Default Behavior
- **Development**: 100% sampling (useful for local debugging)
- **Production**: 1% sampling (cost-effective high-volume production)
- **Staging**: 20% sampling (pre-production validation)
- **QA**: 100% sampling (complete trace visibility for testing)

### Basic Configuration

```kotlin
// In your Activity/Fragment initialization
val viewModel = LandingViewModel()

// Configure for your environment
val environment = BuildConfig.ENVIRONMENT // "production", "staging", "qa", "development"
viewModel.configureSampling(
    environment = parseEnvironment(environment),
    isQaUser = getCurrentUser()?.isQaUser ?: false
)
```

## Environment-Specific Examples

### Production Environment (1% Sampling)

**Goal**: Minimize costs while maintaining visibility for important operations.

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = LandingViewModel()
        viewModel.configureSampling(Environment.PRODUCTION)
        
        // Result: ~1% of requests traced
        // Cost: Minimal, ~1 trace per 100 requests
        // Visibility: Statistically representative
    }
}
```

**Sampling Behavior**:
- 99% of requests: `traceFlags = "00"` (not sampled)
- 1% of requests: `traceFlags = "01"` (sampled)
- All requests send `traceparent` header

### Staging Environment (20% Sampling)

**Goal**: Pre-production validation with reasonable cost.

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = LandingViewModel()
        viewModel.configureSampling(Environment.STAGING)
        
        // Result: ~20% of requests traced
        // Cost: Moderate, ~1 trace per 5 requests
        // Visibility: Good coverage for testing
    }
}
```

**Sampling Behavior**:
- 80% of requests: `traceFlags = "00"` (not sampled)
- 20% of requests: `traceFlags = "01"` (sampled)
- All requests send `traceparent` header

### QA Environment (100% Sampling)

**Goal**: Full visibility for regression testing and validation.

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = LandingViewModel()
        viewModel.configureSampling(Environment.QA)
        
        // Result: 100% of requests traced
        // Cost: Highest, every request generates a trace
        // Visibility: Complete, every operation visible
    }
}
```

**Sampling Behavior**:
- All requests: `traceFlags = "01"` (sampled)
- All requests send `traceparent` header

## Special Cases

### QA Users in Production

**Goal**: Track specific users in production for debugging and support.

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel = LandingViewModel()
        val currentUser = getCurrentUser()
        
        viewModel.configureSampling(
            environment = Environment.PRODUCTION,
            isQaUser = currentUser?.isQaUser ?: false
        )
        
        // Result:
        // - Regular users: 1% sampling
        // - QA users: 100% sampling
    }
}
```

**QA User Detection**:
```kotlin
fun isQaUser(user: User?): Boolean {
    return user?.email?.endsWith("@qa-team.com") ?: false ||
           user?.id in qa_user_ids ||
           user?.tags?.contains("qa-user") ?: false
}
```

### Development Environment (100% Sampling)

**Goal**: Maximum visibility for local development and debugging.

```kotlin
// In your debug build variant
viewModel.configureSampling(Environment.DEVELOPMENT)

// Result: 100% sampling, all requests traced
// Use case: Local debugging, understanding request flows
```

## Advanced Configuration

### Dynamic Sampling Based on User Properties

```kotlin
class UserService(private val viewModel: LandingViewModel) {
    fun configureTracingForUser(user: User) {
        val environment = getEnvironment()
        val isQaUser = isQaUser(user)
        val isBetaTester = user.tags?.contains("beta") ?: false
        val isPowerUser = user.requestCount > 10000
        
        // Beta testers and power users get higher sampling
        val adjustedConfig = if (isBetaTester || isPowerUser) {
            SamplingConfig(
                environment = environment,
                isQaUser = true // Treat as QA for higher sampling
            )
        } else {
            SamplingConfig(
                environment = environment,
                isQaUser = isQaUser
            )
        }
        
        viewModel.configureSampling(adjustedConfig)
    }
}
```

### Environment Detection from BuildConfig

```kotlin
fun getSamplingEnvironment(): Environment {
    return when {
        BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "production" -> 
            Environment.PRODUCTION
        BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "staging" -> 
            Environment.STAGING
        BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "qa" -> 
            Environment.QA
        else -> Environment.DEVELOPMENT
    }
}

// Usage
val viewModel = LandingViewModel()
viewModel.configureSampling(getSamplingEnvironment())
```

### Configuration with Remote Settings

```kotlin
// Fetch sampling configuration from remote settings
suspend fun configureSamplingFromRemote(viewModel: LandingViewModel) {
    val remoteConfig = fetchRemoteConfig() // e.g., Firebase Remote Config
    
    val environment = when (remoteConfig.getString("trace_environment")) {
        "production" -> Environment.PRODUCTION
        "staging" -> Environment.STAGING
        "qa" -> Environment.QA
        else -> Environment.DEVELOPMENT
    }
    
    val sampleRate = remoteConfig.getDouble("trace_sample_rate") // 0.0 - 1.0
    
    // Note: This overrides the default sampling percentages
    val config = SamplingConfig(
        environment = environment,
        isQaUser = remoteConfig.getBoolean("is_qa_user")
    )
    
    viewModel.configureSampling(config)
    
    logConfigured(environment, remoteConfig.getString("trace_sample_rate"))
}
```

## Backend Integration

### Server-Side Sampling Decision

Even though the client sends sampling decisions, the server can override:

```kotlin
// Server-side (Spring Boot example)
@GetMapping("/api/ui/landing")
fun getLanding(
    @RequestHeader(value = "traceparent", required = false) traceparent: String?,
    @RequestHeader(value = "tracestate", required = false) tracestate: String?,
    @RequestAttribute(value = "clientIp", required = false) clientIp: String?
): ResponseEntity<UiComponent> {
    val context = traceContextPropagator.extractContext(traceparent, tracestate)
    
    // Override sampling for important clients
    val shouldSample = if (isImportantClient(clientIp)) {
        true // Always sample important clients
    } else {
        context?.isSampled ?: true // Use client's decision
    }
    
    // Create server span with decision
    val serverSpan = tracer.spanBuilder("GET /api/ui/landing")
        .setSampler(shouldSample)
        .startSpan()
    
    return serverSpan.use {
        ResponseEntity.ok(buildLandingPage())
    }
}
```

### Tail Sampling Strategy

Backend can implement tail sampling to capture errors even if not sampled:

```kotlin
// Server-side tail sampling
fun shouldSampleBasedOnOutcome(span: Span, error: Throwable?): Boolean {
    return when {
        error != null -> true // Always sample errors
        span.durationMs > 5000 -> true // Sample slow requests
        span.statusCode >= 500 -> true // Sample server errors
        span.statusCode == 404 -> false // Don't sample 404s
        else -> span.isSampled // Use original decision
    }
}
```

## Monitoring & Observability

### Log Sampling Configuration

```kotlin
val viewModel = LandingViewModel()
viewModel.configureSampling(Environment.PRODUCTION)

// Logs:
// 🔍 [TRACE] Sampling configured - Environment: PRODUCTION, IsQaUser: false, SampleRate: 1%
```

### Check Current Sampling Status

```kotlin
val context = viewModel.getCurrentTraceContext()
println("Sampled: ${context?.isSampled}")
println("Trace ID: ${context?.traceId}")
println("Sample Rate: ${viewModel.getCurrentSampleRate()}")
```

### Metrics

Track sampling decisions for monitoring:

```kotlin
class TracingMetrics {
    private val sampledRequestsCounter = Counter.builder("trace.sampled.requests")
        .description("Number of sampled requests")
        .build()
    
    private val totalRequestsCounter = Counter.builder("trace.total.requests")
        .description("Total number of requests")
        .build()
    
    fun recordRequest(isSampled: Boolean) {
        totalRequestsCounter.increment()
        if (isSampled) {
            sampledRequestsCounter.increment()
        }
    }
}
```

## Cost Analysis

### Estimated Trace Costs

Assuming:
- 1 million requests/day
- Average trace storage: 1 KB per trace
- Storage cost: $0.01 per GB/month

| Environment | Sample Rate | Traces/Day | Storage/Month | Cost/Month |
|---|---|---|---|---|
| Production | 1% | 10,000 | 300 MB | $0.003 |
| Staging | 20% | 200,000 | 6 GB | $0.06 |
| QA | 100% | 1,000,000 | 30 GB | $0.30 |
| Development | 100% | Variable | Variable | $0.01-0.50 |

## Best Practices

1. **Always send headers**: Even non-sampled requests send `traceparent` header
2. **Backend decision**: Let backend make tail sampling decisions
3. **QA users**: Always sample QA/test users for better debugging
4. **Error tracking**: Server should always sample errors
5. **Cost monitoring**: Track trace storage and costs regularly
6. **Regular reviews**: Update sampling rates based on actual usage and costs

## Troubleshooting

### Issue: Not enough traces in production
**Solution**: Increase production sampling rate if cost allows, or use QA user mode for manual testing

### Issue: Too many traces (high costs)
**Solution**: Decrease sampling rate, implement tail sampling on backend for errors only

### Issue: QA user not being sampled
**Solution**: Check `isQaUser` flag is being set correctly, verify configuration is called before API calls

### Issue: Inconsistent sampling across environments
**Solution**: Verify `configureSampling()` is called with correct environment, check `BuildConfig` values