package com.thinkuldeep.sdui.client.tracing

import kotlin.time.Clock

actual fun currentTimeMillis(): Long {
    // Get current time in milliseconds using Kotlin Clock
    return (Clock.System.now().toEpochMilliseconds())
}