package com.thinkuldeep.sdui.client.threading

actual fun <T> threadSafeExecute(lock: Any, block: () -> T): T {
    return synchronized(lock) {
        block()
    }
}