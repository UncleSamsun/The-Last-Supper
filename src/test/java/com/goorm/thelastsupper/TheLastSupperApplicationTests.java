package com.goorm.thelastsupper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:context-loads;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
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
