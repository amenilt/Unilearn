package com.unilearn.ms1.courses;

import com.unilearn.ms1.courses.domain.Category;
import com.unilearn.ms1.courses.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/unilearn",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/unilearn/protocol/openid-connect/certs",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class MySqlPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("unilearn_courses")
            .withUsername("unilearn_ms1")
            .withPassword("ms1-test-password");

    @Autowired
    private CategoryRepository categoryRepository;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void appliesMySqlMigrationAndPersistsCategories() {
        Category saved = categoryRepository.save(new Category("Test category", "MySQL integration"));

        assertThat(categoryRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(Category::getName)
                .isEqualTo("Test category");
    }
}
