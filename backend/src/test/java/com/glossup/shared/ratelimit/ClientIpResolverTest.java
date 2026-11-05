package com.glossup.shared.ratelimit;

import org.springframework.mock.web.MockHttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void sinXff_usaRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");

        assertEquals("192.168.1.50", resolver.resolver(request));
    }

    @Test
    void conXffYUnProxy_tomaUltimo() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        assertEquals("203.0.113.195", resolver.resolver(request));
    }

    @Test
    void conXffConSpoofingYUnProxy_tomaPenultimoQueIngresAzureAgrego() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // IP del ingress
        // El cliente inyectó 1.1.1.1, pero el proxy agregó 198.51.100.2 al final
        request.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.2");

        assertEquals("198.51.100.2", resolver.resolver(request));
    }

    @Test
    void conTrustedProxiesCero_ignoraXff() {
        ClientIpResolver resolver = new ClientIpResolver(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        assertEquals("127.0.0.1", resolver.resolver(request));
    }

    @Test
    void xffInvalidoOBasura_haceFallbackARemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        request.addHeader("X-Forwarded-For", "javascript:alert(1)");

        assertEquals("192.168.1.10", resolver.resolver(request));
    }

    @Test
    void validacionSintacticaDeIps() {
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("127.0.0.1"));
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("10.0.0.1"));
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("0.0.0.0"));
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("255.255.255.255"));
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("2001:db8::1"));
        org.junit.jupiter.api.Assertions.assertTrue(ClientIpResolver.esIpValida("::1"));

        org.junit.jupiter.api.Assertions.assertFalse(ClientIpResolver.esIpValida("256.0.0.1"));
        org.junit.jupiter.api.Assertions.assertFalse(ClientIpResolver.esIpValida("1.2.3.4.5"));
        org.junit.jupiter.api.Assertions.assertFalse(ClientIpResolver.esIpValida(""));
        org.junit.jupiter.api.Assertions.assertFalse(ClientIpResolver.esIpValida(null));
        org.junit.jupiter.api.Assertions.assertFalse(ClientIpResolver.esIpValida("invalid-ip"));
    }
}
