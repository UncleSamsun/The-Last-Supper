package com.goorm.thelastsupper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.jwt.secret=test-secret-key-for-context-loads",
    "spring.jwt.access-token-expire=1000000000000",
    "spring.jwt.refresh-token-expire=10000000000000",
    "app.redis.initialize=false"
})
class TheLastSupperApplicationTests {

    @Test
    void contextLoads() {
    }

}
