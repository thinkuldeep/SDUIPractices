package com.thinkuldeep.sdui.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SduiServerApplication

fun main(args: Array<String>) {
	runApplication<SduiServerApplication>(*args)
}
