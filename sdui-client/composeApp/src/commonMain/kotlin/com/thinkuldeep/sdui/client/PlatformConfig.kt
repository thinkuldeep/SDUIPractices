package com.thinkuldeep.sdui.client

expect object PlatformConfig {
    val baseUrl: String
    val jaegerEndpoint: String  // For trace exports
    val deviceId: String
    val deviceOs: String
}