package com.thinkuldeep.sdui.client

expect object PlatformConfig {
    val baseUrl: String
    val jaegerEndpoint: String
    val deviceId: String
    val deviceOs: String
}