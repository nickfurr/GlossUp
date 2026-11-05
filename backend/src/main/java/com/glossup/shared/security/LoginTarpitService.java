package com.glossup.shared.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio de infraestructura para retardos progresivos (Tarpitting) en memoria local.
 * Ralentiza las respuestas 401 consecutivas ante fallos de credenciales para desgastar
 * y desarticular ataques de fuerza bruta automatizados.
 */
@Service
public class LoginTarpitService {

    private static final Logger log = LoggerFactory.getLogger(LoginTarpitService.class);

    public static final long DEFAULT_BASE_DELAY_MS = 500L;
    public static final long DEFAULT_MAX_DELAY_MS = 4000L;
    public static final long DEFAULT_EXPIRE_MINUTES = 5L;

    private final Cache<String, AtomicInteger> fallosPorIp;
    private final long baseDelayMs;
    private final long maxDelayMs;

    public LoginTarpitService(
            @Value("${glossup.security.tarpit.base-delay-ms:500}") long baseDelayMs,
            @Value("${glossup.security.tarpit.max-delay-ms:4000}") long maxDelayMs,
            @Value("${glossup.security.tarpit.expire-minutes:5}") long expireMinutes) {
        this.baseDelayMs = Math.max(0L, baseDelayMs);
        this.maxDelayMs = Math.max(this.baseDelayMs, maxDelayMs);
        this.fallosPorIp = Caffeine.newBuilder()
                .expireAfterWrite(Math.max(1L, expireMinutes), TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    /**
     * Incrementa el contador de intentos fallidos para la IP indicada y renueva su expiración.
     *
     * @param ip Dirección IP del cliente
     * @return El número acumulado de intentos fallidos recientes
     */
    public int registrarFallo(String ip) {
        String clave = normalizarClave(ip);
        AtomicInteger contador = fallosPorIp.get(clave, k -> new AtomicInteger(0));
        int intentos = contador.incrementAndGet();
        fallosPorIp.put(clave, contador); // Actualiza la entrada para renovar el expireAfterWrite
        return intentos;
    }

    /**
     * Obtiene el número actual de intentos fallidos registrados para la IP.
     */
    public int obtenerIntentos(String ip) {
        AtomicInteger contador = fallosPorIp.getIfPresent(normalizarClave(ip));
        return contador != null ? contador.get() : 0;
    }

    /**
     * Calcula el retardo en milisegundos según el número de intentos fallidos (fórmula lineal topada).
     */
    public long calcularRetardoMs(String ip) {
        int intentos = obtenerIntentos(ip);
        if (intentos <= 0 || baseDelayMs <= 0) {
            return 0L;
        }
        return Math.min((long) intentos * baseDelayMs, maxDelayMs);
    }

    /**
     * Ejecuta una pausa artificial síncrona en el hilo actual correspondiente al retardo acumulado.
     */
    public void aplicarRetardo(String ip) {
        long delayMs = calcularRetardoMs(ip);
        if (delayMs > 0) {
            log.warn("Tarpitting activo para IP [{}]: aplicando retardo de {} ms tras {} intentos fallidos",
                    ip, delayMs, obtenerIntentos(ip));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Limpia la penalización de una IP (por ejemplo tras un login exitoso).
     */
    public void limpiarFallos(String ip) {
        if (ip != null) {
            fallosPorIp.invalidate(normalizarClave(ip));
        }
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    private static String normalizarClave(String ip) {
        return (ip == null || ip.isBlank()) ? "0.0.0.0" : ip.trim().toLowerCase();
    }
}
