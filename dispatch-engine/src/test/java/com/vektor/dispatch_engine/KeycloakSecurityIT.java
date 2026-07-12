package com.vektor.dispatch_engine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.MountableFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vektor.dispatch_engine.dto.payout.response.SettlementTriggerResponse;

import static org.assertj.core.api.Assertions.assertThat;

import dasniko.testcontainers.keycloak.KeycloakContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "vektor.scheduling.enabled=false",
    "vektor.payout.schedule-ms=60000",
    "vektor.outbox.sweep-ms=60000"
})
@Testcontainers
class KeycloakSecurityIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.0")
            .withDatabaseName("vektor_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
            .withCopyFileToContainer(MountableFile.forHostPath("keycloak/vektor-realm.json"), "/opt/keycloak/data/import/vektor-realm.json");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> 
            keycloak.getAuthServerUrl() + (keycloak.getAuthServerUrl().endsWith("/") ? "" : "/") + "realms/vektor");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getAccessToken(String username, String password) throws Exception {
        String tokenUrl = keycloak.getAuthServerUrl() + (keycloak.getAuthServerUrl().endsWith("/") ? "" : "/") + "realms/vektor/protocol/openid-connect/token";
        String body = "grant_type=password&client_id=vektor-api&client_secret=vektor-api-secret&username=" + username + "&password=" + password;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
            .withFailMessage("Keycloak token fetch failed (status %d): %s", response.statusCode(), response.body())
            .isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(response.body(), Map.class);
        return (String) json.get("access_token");
    }

    @Test
    void opsAdminCanTriggerSettlement() throws Exception {
        String token = getAccessToken("ops-admin", "admin123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<SettlementTriggerResponse> response = restTemplate.postForEntity(
                "/api/v1/payouts/trigger-settlement", entity, SettlementTriggerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void driverPortalCannotTriggerSettlement_returns403() throws Exception {
        String token = getAccessToken("driver-portal", "portal123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/payouts/trigger-settlement", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void driverPortalCanQueryPayouts() throws Exception {
        String token = getAccessToken("driver-portal", "portal123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/payouts/R-101", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anonymousCannotQueryPayouts_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/payouts/R-101", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
