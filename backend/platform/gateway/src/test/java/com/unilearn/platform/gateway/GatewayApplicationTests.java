package com.unilearn.platform.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
class GatewayApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void enablesDiscoveryRoutesOnlyForKnownBusinessServices() {
        assertThat(applicationContext.getBeansOfType(DiscoveryClientRouteDefinitionLocator.class))
                .isNotEmpty();
        assertThat(environment.getProperty(
                "spring.cloud.gateway.server.webflux.discovery.locator.include-expression"
        )).isEqualTo("serviceId matches 'MS[1-6]-(COURSES|QUIZZES|CLAIMS|FORUM|DUELS-BADGES|AI)'");
    }
}
