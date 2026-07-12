package com.vektor.dispatch_engine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vektor.dispatch_engine.config.SecurityConfig;
import com.vektor.dispatch_engine.controller.DeliveryEventController;
import com.vektor.dispatch_engine.controller.DriverPayoutController;
import com.vektor.dispatch_engine.filter.IpRateLimitFilter;
import com.vektor.dispatch_engine.filter.UserRateLimitFilter;
import com.vektor.dispatch_engine.service.DeliveryEventService;
import com.vektor.dispatch_engine.service.DriverPayoutService;

import jakarta.servlet.FilterChain;

@WebMvcTest({DriverPayoutController.class, DeliveryEventController.class, SecurityControllerTest.HealthStubController.class})
@Import(SecurityConfig.class)
class SecurityControllerTest {

    @TestConfiguration
    @RestController
    static class HealthStubController {
        @GetMapping("/actuator/health")
        public ResponseEntity<String> health() {
            return ResponseEntity.ok("{\"status\":\"UP\"}");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private DriverPayoutService driverPayoutService;

    @MockitoBean
    private DeliveryEventService deliveryEventService;

    @MockitoBean
    private IpRateLimitFilter ipRateLimitFilter;

    @MockitoBean
    private UserRateLimitFilter userRateLimitFilter;

    @BeforeEach
    void setupRateLimitFilters() throws Exception {
        Mockito.doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(ipRateLimitFilter).doFilter(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

        Mockito.doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(userRateLimitFilter).doFilter(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void triggerSettlement_requiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/payouts/trigger-settlement")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_payout_read"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payouts/trigger-settlement")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_payout_admin"))))
            .andExpect(status().isOk());
    }

    @Test
    void payoutQuery_rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/payouts/R-101"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void payoutQuery_allowsReadRole() throws Exception {
        mockMvc.perform(get("/api/v1/payouts/R-101")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_payout_read"))))
            .andExpect(status().isOk());
    }

    @Test
    void ingestEvent_requiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"123e4567-e89b-12d3-a456-426614174000\",\"driverId\":\"R-101\",\"status\":\"DELIVERED\",\"distanceKm\":5.0,\"occurredAt\":\"2026-07-12T01:00:00Z\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_payout_read"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void actuatorHealth_staysOpen() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
