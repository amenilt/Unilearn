package com.unilearn.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.profiles.active=peer1",
        "server.port=0",
        "eureka.client.service-url.defaultZone=http://localhost:8762/eureka/"
})
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
