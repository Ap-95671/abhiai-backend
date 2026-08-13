package com.abhiai.abhiai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-only-jwt-secret-that-is-long-enough-for-hs256")
class AbhiaiBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
