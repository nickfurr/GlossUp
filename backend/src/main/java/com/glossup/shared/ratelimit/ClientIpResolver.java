package com.glossup.shared.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Obtiene la IP real del cliente detrás de N proxies de confianza (p. ej. el ingress de Azure Container Apps).
 */
public final class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F:]+$");

    private final int trustedProxies;

    public ClientIpResolver(int trustedProxies) {
        this.trustedProxies = Math.max(0, trustedProxies);
    }

    public String resolver(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (trustedProxies > 0 && xff != null && !xff.isBlank()) {
            String[] saltos = xff.split(",");
            // El proxy de confianza (último) añade la IP al final.
            // Se toma la N-ésima desde la derecha:
            int idx = saltos.length - trustedProxies;
            if (idx >= 0) {
                String ip = saltos[idx].trim();
                if (esIpValida(ip)) {
                    return normalizar(ip);
                }
            }
        }
        return normalizar(req.getRemoteAddr());
    }

    public static boolean esIpValida(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || (ip.contains(":") && IPV6_PATTERN.matcher(ip).matches());
    }

    private static String normalizar(String ip) {
        if (ip == null) {
            return "0.0.0.0";
        }
        String limpia = ip.trim().toLowerCase();
        int zoneIdx = limpia.indexOf('%');
        if (zoneIdx > 0) {
            limpia = limpia.substring(0, zoneIdx);
        }
        return limpia;
    }
}
