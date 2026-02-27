package com.thinkuldeep.sdui_client

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform