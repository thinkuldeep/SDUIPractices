package com.thinkuldeep.sdui.client

import kotlinx.browser.window

actual object PlatformConfig {
    actual val baseUrl: String =
        "${window.location.protocol}//${window.location.hostname}:8080"
}