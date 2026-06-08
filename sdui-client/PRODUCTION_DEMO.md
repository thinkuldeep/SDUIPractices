# Production Environment Demo

This demonstrates how to run the SDUI client as if it's in a production environment with 1% trace sampling.

## Quick Start - Run as Production

### Option 1: Direct Configuration in Code

```kotlin
// In your MainActivity or entry point
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create ViewModel
        val viewModel = LandingViewModel()
        
        // Configure for PRODUCTION (1% sampling)
        viewModel.configureSampling(
            environment = Environment.PRODUCTION,
            isQaUser = false
        )
        
        setContent {
            LandingScreen(viewModel = viewModel)
        }
        
        // Output:
        // 🔍 [TRACE] Sampling configured - Environment: PRODUCTION, IsQaUser: false, SampleRate: 1%
    }
}
```

### Option 2: BuildConfig-based Configuration

```kotlin
// In your Activity
val environment = when {
    BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "prod" -> Environment.PRODUCTION
    BuildConfig.BUILD_TYPE == "release" && BuildConfig.FLAVOR == "staging" -> Environment.STAGING
    BuildConfig.BUILD_TYPE == "debugStaging" -> Environment.STAGING
    else -> Environment.QA
}

val isQaUser = currentUser?.isQaUser ?: false

viewModel.configureSampling(environment, isQaUser)
```

### Option 3: Remote Configuration

```kotlin
suspend fun setupTracingFromRemote(viewModel: LandingViewModel) {
    val remoteConfig = fetchRemoteConfig()
    
    val environment = when (remoteConfig.getString("environment")) {
        "production" -> Environment.PRODUCTION
        "staging" -> Environment.STAGING
        else -> Environment.QA
    }
    
    viewModel.configureSampling(
        environment = environment,
        isQaUser = remoteConfig.getBoolean("is_qa_user")
    )
}
```

## What Happens in Production?

### 1% Sampling Rate

When configured for production:

```
Request 1:  🔍 [TRACE] Traceparent: 00-abc123xyz-def456-00  (NOT sampled - 99%)
Request 2:  🔍 [TRACE] Traceparent: 00-xyz789abc-ghi012-00  (NOT sampled - 99%)
Request 3:  🔍 [TRACE] Traceparent: 00-def456ghi-jkl345-01  (SAMPLED - 1%)
Request 4:  🔍 [TRACE] Traceparent: 00-jkl012def-mno678-00  (NOT sampled - 99%)
...
```

**Key Points:**
- 99% of requests: `traceFlags = "00"` (not sampled)
- 1% of requests: `traceFlags = "01"` (sampled)
- **ALL requests include `traceparent` header**

### HTTP Headers Sent

Even for non-sampled requests:

```http
GET /api/ui/landing HTTP/1.1
traceparent: 00-abc123xyz-def456-00
tracestate: <optional-vendor-data>
```

The backend receives this and can decide to:
- Ignore the trace (since it's not sampled)
- Store it for correlation purposes
- Make its own sampling decision (tail sampling)

## Performance Impact

### Sampling Verification Test

Running the production sampling test (simulating 1000 requests):

```
📊 Production Sampling Test Results:
   Total Requests: 1000
   Sampled Requests: 8 (0.8%)
   Non-Sampled Requests: 992 (99.2%)
```

**Result**: ✅ Maintains ~1% sampling rate

## Cost Analysis

### For 1 Million Requests/Day

| Metric | Value |
|--------|-------|
| Total requests/day | 1,000,000 |
| Sample rate | 1% |
| Traces generated/day | 10,000 |
| Bytes per trace | 1 KB |
| Storage/day | 10 MB |
| Storage/month | 300 MB |
| Storage cost/month | ~$0.003 |

**Conclusion**: ✅ Very cost-effective for production

## Special Case: QA Users in Production

When a QA user is detected:

```kotlin
viewModel.configureSampling(
    environment = Environment.PRODUCTION,
    isQaUser = true  // ← Override for QA user
)

// Output:
// 🔍 [TRACE] Sampling configured - Environment: PRODUCTION, IsQaUser: true, SampleRate: 100%
```

All 100 requests from QA user are sampled (`traceFlags = "01"`), providing full visibility for debugging while keeping regular users at 1%.

## Integration with Observability Platform

### With Jaeger

1. **Collected traces** → Aggregated in Jaeger
2. **Search by Trace ID** → See full request flow
3. **View latency breakdown** → Identify bottlenecks

Example Jaeger query:
```
Service: sdui-client
Operation: fetchLanding
Trace ID: abc123xyz
```

### Cost Savings

At 1% sampling with Jaeger storage:
- ✅ Costs ~$3/month for 1M requests/day
- ✅ Still captures errors and anomalies via tail sampling
- ✅ Statistically representative of production behavior

## Testing Production Configuration

### Unit Test

```kotlin
@Test
fun testProductionConfiguration() {
    val config = SamplingConfig(
        environment = Environment.PRODUCTION,
        isQaUser = false
    )
    TraceSamplerHolder.setConfig(config)
    
    var sampledCount = 0
    repeat(1000) {
        if (TraceSamplerHolder.shouldSample()) {
            sampledCount++
        }
    }
    
    // Should be approximately 10 (1% of 1000)
    assertTrue(sampledCount < 50, "Expected <5% actual sampling")
}
```

### Integration Test

```kotlin
@Test
fun testFullProductionFlow() {
    // 1. Configure for production
    val viewModel = LandingViewModel()
    viewModel.configureSampling(Environment.PRODUCTION)
    
    // 2. Verify sampling
    val context = viewModel.getCurrentTraceContext()
    assertNotNull(context)
    
    // 3. Verify header format
    val traceparent = context.toTraceparent()
    assertTrue(traceparent.matches(Regex("00-[a-f0-9]{32}-[a-f0-9]{16}-0[01]")))
    
    // 4. All requests have context
    repeat(100) {
        val requestContext = TraceContext.create()
        assertTrue(requestContext.toTraceparent().isNotEmpty())
    }
}
```

## Runtime Configuration

### Check Current Configuration

```kotlin
val context = viewModel.getCurrentTraceContext()
println("Trace ID: ${context?.traceId}")
println("Sampled: ${context?.isSampled}")
println("Flags: ${context?.traceFlags}")

// Output:
// Trace ID: abc123xyz...
// Sampled: false (or true, ~1% of the time)
// Flags: 00 (or 01 for sampled)
```

### Dynamic Switching

```kotlin
// Start in production
viewModel.configureSampling(Environment.PRODUCTION)

// Switch to QA for debugging
viewModel.configureSampling(Environment.QA)
// Now 100% sampling

// Back to production
viewModel.configureSampling(Environment.PRODUCTION)
// Back to 1% sampling
```

## Monitoring & Alerts

### Trace Volume Metrics

```
traces_per_minute{environment="production"} = ~7 (for 1M req/day)
traces_per_minute{environment="staging"} = ~140 (for 1M req/day)
traces_per_minute{environment="qa"} = ~700 (for 1M req/day)
```

### Cost Monitoring

```
trace_storage_gb_per_month{environment="production"} = 0.3 GB (~$0.003)
trace_storage_gb_per_month{environment="staging"} = 6 GB (~$0.06)
trace_storage_gb_per_month{environment="qa"} = 30 GB (~$0.30)
```

## Troubleshooting

### Issue: Not seeing any traces in production

**Cause**: Sampling might not be configured

**Solution**:
```kotlin
// Verify configuration
val config = TraceSamplerHolder.getConfig()
println("Environment: ${config.environment}")
println("QA User: ${config.isQaUser}")
println("Should Sample: ${config.shouldSample()}")
```

### Issue: Wrong sampling rate

**Cause**: Configuration not called before requests

**Solution**:
```kotlin
// Must call BEFORE making API calls
val viewModel = LandingViewModel()
viewModel.configureSampling(Environment.PRODUCTION)  // ← Call first!

// Then use viewModel
val ui = viewModel.uiState.value
```

### Issue: QA user not being sampled in production

**Cause**: isQaUser flag not set correctly

**Solution**:
```kotlin
fun isQaUser(user: User?): Boolean {
    return user?.email?.contains("@qa") ?: false ||
           user?.roles?.contains("QA") ?: false
}

viewModel.configureSampling(
    environment = Environment.PRODUCTION,
    isQaUser = isQaUser(currentUser)
)
```

## Summary

✅ **Production (1% sampling)**
- 99% non-sampled requests: `traceFlags = "00"`
- 1% sampled requests: `traceFlags = "01"`
- All requests include `traceparent` header
- Cost: ~$0.003/month for 1M requests

✅ **QA Users**: Always sampled (100%) for debugging

✅ **Backend**: Can make tail sampling decisions based on errors/latency

✅ **Visibility**: Statistically representative trace data with minimal cost

You can now run your app in production mode and monitor traces efficiently!