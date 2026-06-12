# Event Tracking with EventTracker

Event tracking with distributed trace context. All span management handled by platform-specific implementations (Swift on iOS, Firebase on Android).

## Architecture

The EventTracker follows the expect/actual pattern:
- **Common code** (`commonMain/observability/EventTracker.kt`) — Event types, data models, and expect class
- **iOS** (`iosMain/observability/EventTracker.kt`) — Thin bridge to Swift
- **Swift** (`iosApp/iosApp/SwiftEventTracker.swift`) — All span lifecycle management
- **Android** (`androidMain/observability/EventTracker.kt`) — Direct implementation with Firebase

## API

### EventTracker Methods

#### 1. Track Event Started

```kotlin
fun trackEventStarted(
    operationName: String,
    properties: Map<String, String> = emptyMap()
): EventSpan
```

**Returns:** EventSpan with traceId, spanId (for downstream use)

**What happens:**
- Creates new span in Swift/Android
- Stores span state (start time, operation name)
- Returns trace context for propagation downstream

#### 2. Track Event Completed

```kotlin
fun trackEventCompleted(
    startedSpan: EventSpan,
    properties: Map<String, String> = emptyMap()
): EventSpan
```

**Parameters:** EventSpan from trackEventStarted

**Returns:** EventSpan with durationMs calculated

**What happens:**
- Finds span by traceId-spanId key
- Calculates duration (Date().now - startTime)
- Cleans up span state
- Returns completed span with duration

#### 3. Track Error

```kotlin
fun trackError(
    operationName: String,
    errorMessage: String,
    startedSpan: EventSpan? = null,
    properties: Map<String, String> = emptyMap()
): EventSpan
```

**Parameters:**
- `startedSpan`: Optional span to end (if operation was started)

**Returns:** EventSpan marked as ERROR type

**What happens:**
- Records error in analytics
- Ends span if provided
- Returns error span with trace context

#### 4. Set User ID

```kotlin
fun setUserId(userId: String)
```

## Usage Examples

### Basic Flow

```kotlin
import com.thinkuldeep.sdui.client.observability.EventTrackerProvider
import com.thinkuldeep.sdui.client.observability.EventType

val tracker = EventTrackerProvider.instance

// Start operation
val startedSpan = tracker.trackEventStarted("fetch_user_data")
println("Trace: ${startedSpan.traceId}, Span: ${startedSpan.spanId}")

// Get trace context for downstream
val context = startedSpan.toTraceContext()
val headers = context.toHeaders()

// Make HTTP request with trace headers
httpClient.get("/api/user") {
    headers.forEach { (k, v) -> header(k, v) }
}

// Complete operation
val completedSpan = tracker.trackEventCompleted(startedSpan)
println("Duration: ${completedSpan.durationMs}ms")
```

### With Error Handling

```kotlin
val tracker = EventTrackerProvider.instance

try {
    val startedSpan = tracker.trackEventStarted("process_payment")
    
    // Do work...
    processPayment()
    
    tracker.trackEventCompleted(startedSpan)
} catch (e: Exception) {
    tracker.trackError(
        operationName = "process_payment",
        errorMessage = e.message ?: "Unknown error",
        startedSpan = startedSpan,  // Optional: ends the span
        properties = mapOf("errorType" to e::class.simpleName)
    )
}
```

### Nested Operations

```kotlin
val tracker = EventTrackerProvider.instance

// Parent operation
val parentSpan = tracker.trackEventStarted("fetch_and_process")

try {
    // Child operation 1
    val childSpan1 = tracker.trackEventStarted(
        "fetch_data",
        mapOf("traceId" to parentSpan.traceId)
    )
    fetchData()
    tracker.trackEventCompleted(childSpan1)
    
    // Child operation 2
    val childSpan2 = tracker.trackEventStarted(
        "process_data",
        mapOf("traceId" to parentSpan.traceId)
    )
    processData()
    tracker.trackEventCompleted(childSpan2)
    
    // Complete parent
    tracker.trackEventCompleted(parentSpan)
} catch (e: Exception) {
    tracker.trackError("fetch_and_process", e.message ?: "", parentSpan)
}
```

### With Custom Properties

```kotlin
val tracker = EventTrackerProvider.instance

val startedSpan = tracker.trackEventStarted(
    "database_query",
    mapOf(
        "query_type" to "SELECT",
        "table_name" to "users",
        "row_count" to "100"
    )
)

// Do work...

tracker.trackEventCompleted(
    startedSpan,
    mapOf(
        "result_count" to "95",
        "cache_hit" to "false"
    )
)
```

## Data Models

### EventSpan

```kotlin
data class EventSpan(
    val traceId: String,
    val spanId: String,
    val eventType: EventType,
    val operationName: String,
    val traceFlags: String,        // "01" = sampled, "00" = not
    val isSampled: Boolean,
    val environment: String,
    val durationMs: Long? = null   // Only for completed/error spans
)
```

### TraceContext

```kotlin
data class TraceContext(
    val traceId: String,
    val spanId: String,
    val traceFlags: String,
    val isSampled: Boolean
) {
    fun toTraceparent(): String  // W3C format: "00-traceId-spanId-flags"
    fun toHeaders(): Map<String, String>  // HTTP headers
}
```

### EventType

```kotlin
enum class EventType {
    EVENT_STARTED,
    EVENT_COMPLETED,
    ERROR
}
```

## Platform-Specific Details

### iOS (Swift)

- **Span Storage:** In-memory map (`activeSpans`)
- **Duration:** Calculated using `Date()` start/end
- **Analytics:** Delegates to `AnalyticsManager` (customize for Crashlytics, Segment, etc.)
- **User ID:** Delegates to `UserManager`

### Android

- **Span Storage:** In-memory map (`activeSpans`)
- **Duration:** Calculated using `System.currentTimeMillis()`
- **Analytics:** Firebase Analytics
- **User ID:** Firebase Analytics setUserId

## Span Lifecycle

```
trackEventStarted()
    ↓ (creates span in Swift/Android, stores with startTime)
    ├─ traceId: unique identifier
    ├─ spanId: operation identifier
    ├─ traceFlags: "01" (sampled) or "00"
    └─ (return EventSpan)

[do work, make downstream calls]

trackEventCompleted(startedSpan)
    ↓ (finds span by traceId-spanId)
    ├─ calculate duration
    ├─ remove from activeSpans
    └─ return completed EventSpan with durationMs

OR

trackError(..., startedSpan)
    ↓ (if startedSpan provided)
    ├─ remove from activeSpans
    └─ return error EventSpan
```

## Debugging

### Check Active Spans (iOS)

In Swift, add debug method:
```swift
func debugActiveSpans() {
    print("Active spans: \(activeSpans.count)")
    activeSpans.forEach { key, state in
        print("  \(key): \(state.operationName)")
    }
}
```

### Check Active Spans (Android)

```kotlin
fun debugActiveSpans() {
    println("Active spans: ${activeSpans.size}")
    activeSpans.forEach { (key, _) -> println("  $key") }
}
```

### Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Span not found on complete | `startedSpan.spanId` doesn't match | Use exact EventSpan returned from trackEventStarted |
| Duration always 0 | Completed immediately after started | Check if work actually happens between start/complete |
| Missing trace context | Event was not started | Always call trackEventStarted first |
| Wrong environment | Debug vs release build | Check build variant configuration |
