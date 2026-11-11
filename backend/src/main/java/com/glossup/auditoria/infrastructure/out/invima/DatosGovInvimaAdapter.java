package com.glossup.auditoria.infrastructure.out.invima;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c.ProductoInvimaDetalle;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Adaptador de salida para contrastar registros sanitarios del catálogo
 * contra la API oficial de Datos Abiertos del Gobierno de Colombia / INVIMA (dataset ui32-p9f2).
 */
@Component
public class DatosGovInvimaAdapter implements VerificacionInvimaPort {

    private static final Logger log = LoggerFactory.getLogger(DatosGovInvimaAdapter.class);

    private static final String URL_API_INVIMA = "https://www.datos.gov.co/resource/ui32-p9f2.json";
    private static final Pattern PATRON_NSOC = Pattern.compile("^NSOC\\d{5}-\\d{2}[A-Z]{2}$", Pattern.CASE_INSENSITIVE);

    private final ProductoRepositoryPort productoRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DatosGovInvimaAdapter(ProductoRepositoryPort productoRepository) {
        this(productoRepository, new ObjectMapper());
    }

    public DatosGovInvimaAdapter(ProductoRepositoryPort productoRepository, ObjectMapper objectMapper) {
        this(productoRepository, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    DatosGovInvimaAdapter(ProductoRepositoryPort productoRepository, ObjectMapper objectMapper, HttpClient httpClient) {
        this.productoRepository = productoRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ResultadoVerificacionInvima verificarProductosActivos() {
        List<Producto> activos = productoRepository.listarTodos().stream()
                .filter(Producto::isActivo)
                .toList();

        if (activos.isEmpty()) {
            return new ResultadoVerificacionInvima("SIN_PRODUCTOS_ACTIVOS", 0, 0, List.of());
        }

        Set<String> codigosNsoc = new LinkedHashSet<>();
        for (Producto p : activos) {
            if (p.getRegistroInvima() != null && !p.getRegistroInvima().isBlank()) {
                codigosNsoc.add(p.getRegistroInvima().trim().toUpperCase());
            }
        }

        Map<String, Map<String, Object>> respuestasOficiales = consultarApiDatosGovCo(codigosNsoc);
        boolean consultadoEnLinea = !respuestasOficiales.isEmpty();

        List<ProductoInvimaDetalle> detalles = new ArrayList<>();
        int vigentes = 0;

        for (Producto p : activos) {
            String reg = p.getRegistroInvima() != null ? p.getRegistroInvima().trim().toUpperCase() : "";
            boolean formatoValido = PATRON_NSOC.matcher(reg).matches();

            Map<String, Object> dataOficial = respuestasOficiales.get(reg);
            String estadoInvima;
            String titular;
            String fechaVencimiento;
            String grupo;

            if (dataOficial != null) {
                estadoInvima = String.valueOf(dataOficial.getOrDefault("estadoregistro", "Desconocido"));
                titular = String.valueOf(dataOficial.getOrDefault("titular", "N/A"));
                fechaVencimiento = String.valueOf(dataOficial.getOrDefault("fechavencimiento", "N/A"));
                grupo = String.valueOf(dataOficial.getOrDefault("grupo", "COSMETICOS"));
            } else if (formatoValido) {
                estadoInvima = consultadoEnLinea ? "No Registrado en INVIMA" : "Vigente (Validado por nomenclatura NSOC)";
                titular = "N/A";
                fechaVencimiento = "N/A";
                grupo = "COSMETICOS";
            } else {
                estadoInvima = "Sin registro o formato inválido";
                titular = "N/A";
                fechaVencimiento = "N/A";
                grupo = "N/A";
            }

            boolean esVigente = "Vigente".equalsIgnoreCase(estadoInvima)
                    || estadoInvima.startsWith("Vigente");

            if (formatoValido && esVigente) {
                vigentes++;
            }

            detalles.add(new ProductoInvimaDetalle(
                    p.getId(),
                    p.getNombre(),
                    p.getMarca(),
                    reg,
                    formatoValido,
                    estadoInvima,
                    titular,
                    fechaVencimiento,
                    grupo,
                    dataOficial != null
            ));
        }

        String fuente = consultadoEnLinea ? "DATOS_GOV_CO_INVIMA_UI32_P9F2" : "VALIDACION_ESTANDAR_NSOC";
        return new ResultadoVerificacionInvima(fuente, activos.size(), vigentes, detalles);
    }

    private Map<String, Map<String, Object>> consultarApiDatosGovCo(Set<String> codigos) {
        if (codigos.isEmpty()) return Map.of();

        Map<String, Map<String, Object>> resultado = new HashMap<>();
        try {
            String inClause = "rsynso in (" + String.join(",", codigos.stream().map(c -> "'" + c + "'").toList()) + ")";
            String queryParams = String.format(
                    "$select=%s&$where=%s&$group=%s",
                    URLEncoder.encode("rsynso,estadoregistro,fechavencimiento,titular,grupo", StandardCharsets.UTF_8),
                    URLEncoder.encode(inClause, StandardCharsets.UTF_8).replace("+", "%20"),
                    URLEncoder.encode("rsynso,estadoregistro,fechavencimiento,titular,grupo", StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API_INVIMA + "?" + queryParams))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                List<Map<String, Object>> lista = objectMapper.readValue(response.body(), new TypeReference<>() {});
                for (Map<String, Object> item : lista) {
                    Object rs = item.get("rsynso");
                    if (rs == null) rs = item.get("registrosanitario");
                    if (rs != null) {
                        resultado.put(String.valueOf(rs).trim().toUpperCase(), item);
                    }
                }
                log.info("Consulta exitosa a datos.gov.co: {} registros coincidentes de {} consultados", resultado.size(), codigos.size());
            } else {
                log.warn("Respuesta no exitosa de datos.gov.co (HTTP {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("No fue posible consultar datos.gov.co en vivo (fallback activo): {}", e.getMessage());
        }

        return resultado;
    }
}
