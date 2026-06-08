package com.thinkuldeep.sdui.client.tracing

import kotlin.time.TimeSource

actual fun currentTimeMillis(): Long {
    return TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
}