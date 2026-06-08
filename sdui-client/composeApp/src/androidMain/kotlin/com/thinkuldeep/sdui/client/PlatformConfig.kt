package com.thinkuldeep.sdui.client

import android.os.Build
import java.util.UUID

actual object PlatformConfig {
    actual val baseUrl: String = "http://10.0.2.2:8080"

    // Jaeger OTLP HTTP receiver endpoint (more reliable than /api/traces)
    actual val jaegerEndpoint: String = "http://10.0.2.2:4318/v1/traces"

    actual val deviceId: String = UUID.randomUUID().toString()

    actual val deviceOs: String = "android"
}