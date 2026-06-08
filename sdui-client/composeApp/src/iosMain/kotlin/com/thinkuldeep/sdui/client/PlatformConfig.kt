package com.thinkuldeep.sdui.client

import platform.UIKit.UIDevice
import platform.Foundation.NSUUID

actual object PlatformConfig {
    actual val baseUrl: String = "http://localhost:8080"

    // Jaeger OTLP HTTP receiver endpoint
    actual val jaegerEndpoint: String = "http://localhost:4318/v1/traces"

    actual val deviceId: String = UIDevice.currentDevice.identifierForVendor?.UUIDString
        ?: NSUUID().UUIDString

    actual val deviceOs: String = "ios"
}