package com.unilearn.quiz;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.profiles.active=h2",
        "eureka.client.enabled=false"
})
class QuizServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
