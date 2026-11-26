package tn.esprit.unilearn.reclamation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * MS3 - Reclamations (UniLearn).
 * Responsable : Mohamed Rhouma.
 */
@SpringBootApplication
@EnableDiscoveryClient // client Eureka (facultatif avec Spring Cloud recent, garde pour rendre le role explicite)
public class ReclamationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReclamationApplication.class, args);
    }

    /**
     * RestTemplate "load balance" : l'URL http://MS1-COURS/... est resolue par Eureka
     * (decouverte cote client) et les appels sont repartis entre les instances de MS1.
     */
    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
