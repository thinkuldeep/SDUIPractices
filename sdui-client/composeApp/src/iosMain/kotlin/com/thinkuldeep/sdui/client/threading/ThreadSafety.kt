package com.thinkuldeep.sdui.client.threading

actual fun <T> threadSafeExecute(lock: Any, block: () -> T): T {
    // iOS is primarily single-threaded for UI operations
    // but we execute the block directly for consistency
    return block()
}