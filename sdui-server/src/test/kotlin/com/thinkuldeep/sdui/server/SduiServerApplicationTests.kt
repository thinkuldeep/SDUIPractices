package com.thinkuldeep.sdui.server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SduiServerApplicationTests {

	@Test
	fun contextLoads() {
	}

	@Test
	fun `main function starts application context`() {
		main(arrayOf("--spring.main.web-application-type=none"))
	}

}
