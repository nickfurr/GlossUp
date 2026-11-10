package com.glossup.auditoria.infrastructure.out.pci;

import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Adaptador de salida para ejecutar auditoría forense PCI-DSS en PostgreSQL y logs del servidor.
 */
@Component
public class PostgresForensePciAdapter implements AuditoriaForensePciPort {

    private static final Logger log = LoggerFactory.getLogger(PostgresForensePciAdapter.class);

    private static final Set<String> TERMINOS_PROHIBIDOS = Set.of(
            "pan", "cvv", "cvc", "ccv", "card_number", "numero_tarjeta",
            "tarjeta_numero", "pin_block", "track1", "track2", "codigo_seguridad",
            "expiracion_tarjeta", "fecha_expiracion_tarjeta"
    );

    private static final List<String> COLUMNAS_PASARELA_ESPERADAS = List.of(
            "pedido.pago_transaccion_id",
            "pedido.pago_metodo",
            "pedido.pagado_en"
    );

    private static final Pattern PATRON_PAN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b");

    private final JdbcTemplate jdbcTemplate;

    public PostgresForensePciAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ResultadoAuditoriaPci ejecutarAuditoria() {
        // 1. Inspeccionar information_schema.columns
        Set<String> tablas = new HashSet<>();
        List<String> columnasProhibidas = new ArrayList<>();
        int totalColumnas = 0;

        try {
            String sqlSchema = """
                    SELECT table_name, column_name
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                    """;

            List<Map<String, Object>> filas = jdbcTemplate.queryForList(sqlSchema);
            totalColumnas = filas.size();

            for (Map<String, Object> fila : filas) {
                String tabla = String.valueOf(fila.get("table_name")).toLowerCase();
                String columna = String.valueOf(fila.get("column_name")).toLowerCase();
                tablas.add(tabla);

                for (String prohibido : TERMINOS_PROHIBIDOS) {
                    if (columna.equals(prohibido) || columna.contains("_" + prohibido) || columna.startsWith(prohibido + "_")) {
                        columnasProhibidas.add(tabla + "." + columna);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al consultar information_schema: {}", e.getMessage());
        }

        // 2. Verificar datos en tabla pedido
        long totalPedidos = 0;
        long pedidosConPan = 0;
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pedido", Long.class);
            totalPedidos = count != null ? count : 0;

            String sqlPedidos = "SELECT pago_transaccion_id, pago_metodo FROM pedido WHERE pago_transaccion_id IS NOT NULL";
            List<Map<String, Object>> pedidos = jdbcTemplate.queryForList(sqlPedidos);
            for (Map<String, Object> p : pedidos) {
                String tx = String.valueOf(p.get("pago_transaccion_id"));
                String met = String.valueOf(p.get("pago_metodo"));
                if (contienePan(tx) || contienePan(met)) {
                    pedidosConPan++;
                }
            }
        } catch (Exception e) {
            log.warn("No fue posible inspeccionar filas de pedido: {}", e.getMessage());
        }

        // 3. Escaneo de logs del servidor
        int[] resultadoLogs = escanearLogs();
        int archivosLog = resultadoLogs[0];
        int fugasLogs = resultadoLogs[1];

        return new ResultadoAuditoriaPci(
                tablas.size(),
                totalColumnas,
                columnasProhibidas,
                COLUMNAS_PASARELA_ESPERADAS,
                totalPedidos,
                pedidosConPan,
                archivosLog,
                fugasLogs
        );
    }

    private static boolean contienePan(String texto) {
        if (texto == null || texto.isBlank()) return false;
        Matcher m = PATRON_PAN.matcher(texto);
        while (m.find()) {
            String digits = m.group().replaceAll("[\\s-]", "");
            if (pasaLuhn(digits)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pasaLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private int[] escanearLogs() {
        int archivos = 0;
        int fugas = 0;
        List<Path> carpetasLog = List.of(
                Path.of("logs"),
                Path.of("/app/logs"),
                Path.of(".")
        );

        for (Path c : carpetasLog) {
            if (Files.isDirectory(c)) {
                try (Stream<Path> stream = Files.walk(c, 1)) {
                    List<Path> logs = stream.filter(p -> p.toString().endsWith(".log")).toList();
                    for (Path logPath : logs) {
                        archivos++;
                        try {
                            List<String> lineas = Files.readAllLines(logPath);
                            for (String l : lineas) {
                                if (contienePan(l) || l.toLowerCase().contains("cvv=") || l.toLowerCase().contains("cvc=")) {
                                    fugas++;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }
        return new int[]{archivos, fugas};
    }
}
