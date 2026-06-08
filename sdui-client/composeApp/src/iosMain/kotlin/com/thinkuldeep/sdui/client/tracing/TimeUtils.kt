package com.thinkuldeep.sdui.client.tracing

import platform.Foundation.NSDate

actual fun currentTimeMillis(): Long {
    // Get actual Unix timestamp in milliseconds
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}