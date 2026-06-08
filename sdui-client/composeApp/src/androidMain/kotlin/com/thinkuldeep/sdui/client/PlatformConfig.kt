package com.thinkuldeep.sdui.client

import android.os.Build
import java.util.UUID

actual object PlatformConfig {
    actual val baseUrl: String = "http://10.0.2.2:8080"

    actual val jaegerEndpoint: String = "http://10.0.2.2:14268/api/traces"

    actual val deviceId: String = UUID.randomUUID().toString()

    actual val deviceOs: String = "android"
}