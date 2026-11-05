package com.glossup.shared.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = RateLimitConfig.class)
@TestPropertySource(properties = {
        "glossup.rate-limit.endpoints.[/api/auth/login].capacity=10",
        "glossup.rate-limit.endpoints.[/api/auth/login].refill-tokens=10",
        "glossup.rate-limit.endpoints.[/api/auth/login].refill-period=1m"
})
class RateLimitPropertiesBindingTest {

    @Autowired
    private RateLimitProperties properties;

    @Test
    void bindingDeMapEndpoints() {
        assertNotNull(properties);
        System.out.println("Endpoints keys in map: " + properties.endpoints().keySet());
        RateLimitProperties.LimitProperties limit = properties.limiteDe("/api/auth/login");
        assertEquals(10, limit.capacity(), "Debe obtener el limite especifico de 10 para /api/auth/login");
    }
}
