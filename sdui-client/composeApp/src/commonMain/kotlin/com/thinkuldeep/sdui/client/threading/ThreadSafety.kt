package com.thinkuldeep.sdui.client.threading

expect fun <T> threadSafeExecute(lock: Any, block: () -> T): T