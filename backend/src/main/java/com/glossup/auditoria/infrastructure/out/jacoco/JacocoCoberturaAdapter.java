package com.glossup.auditoria.infrastructure.out.jacoco;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24.ModuloCobertura;
import com.glossup.auditoria.domain.port.out.CoberturaCodigoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida para leer y procesar métricas de JaCoCo.
 */
@Component
public class JacocoCoberturaAdapter implements CoberturaCodigoPort {

    private static final Logger log = LoggerFactory.getLogger(JacocoCoberturaAdapter.class);

    @Override
    public DatosCobertura obtenerCobertura(boolean forzarEjecucionMaven) {
        if (forzarEjecucionMaven) {
            ejecutarMavenSiDisponible();
        }

        BufferedReader reader = resolverLectorCsv();
        if (reader == null) {
            log.warn("No se encontró jacoco.csv en disco ni en classpath");
            return new DatosCobertura(0, 0, 0, 0, "NO_DISPONIBLE", List.of());
        }

        int covPerfil = 0, misPerfil = 0;
        int covGlobal = 0, misGlobal = 0;
        Map<String, int[]> modulos = new LinkedHashMap<>();

        try (reader) {
            String header = reader.readLine();
            if (header == null) {
                return new DatosCobertura(0, 0, 0, 0, "CSV_VACIO", List.of());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",");
                if (cols.length < 10) continue;

                String pkg = cols[1];
                try {
                    int lineMissed = Integer.parseInt(cols[7]);
                    int lineCovered = Integer.parseInt(cols[8]);

                    if (esCapaDominioOAplicacion(pkg)) {
                        covGlobal += lineCovered;
                        misGlobal += lineMissed;

                        String modulo = extraerModulo(pkg);
                        modulos.computeIfAbsent(modulo, k -> new int[2]);
                        modulos.get(modulo)[0] += lineCovered;
                        modulos.get(modulo)[1] += lineMissed;

                        if (pkg.startsWith("com.glossup.perfil.")) {
                            covPerfil += lineCovered;
                            misPerfil += lineMissed;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            log.error("Error al procesar jacoco.csv: {}", e.getMessage());
        }

        List<ModuloCobertura> desglose = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : modulos.entrySet()) {
            int c = entry.getValue()[0];
            int m = entry.getValue()[1];
            double pct = calcularPct(c, m);
            desglose.add(new ModuloCobertura(entry.getKey(), c, m, pct));
        }

        return new DatosCobertura(
                covPerfil, misPerfil,
                covGlobal, misGlobal,
                "JACOCO_BYTECODE_CSV",
                desglose
        );
    }

    private static boolean esCapaDominioOAplicacion(String pkg) {
        return pkg.contains(".domain.") || pkg.endsWith(".domain")
                || pkg.contains(".application.") || pkg.endsWith(".application");
    }

    private static String extraerModulo(String pkg) {
        String[] parts = pkg.split("\\.");
        return (parts.length >= 3 && "com".equals(parts[0]) && "glossup".equals(parts[1]))
                ? parts[2]
                : pkg;
    }

    private static double calcularPct(int c, int m) {
        int t = c + m;
        if (t == 0) return 0.0;
        return BigDecimal.valueOf((double) c * 100.0 / t)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BufferedReader resolverLectorCsv() {
        List<Path> candidatos = List.of(
                Path.of("target/site/jacoco/jacoco.csv"),
                Path.of("PYF/backend/target/site/jacoco/jacoco.csv"),
                Path.of("/app/jacoco.csv")
        );

        for (Path p : candidatos) {
            if (Files.exists(p) && Files.isReadable(p)) {
                try {
                    log.info("Leyendo métricas JaCoCo desde {}", p.toAbsolutePath());
                    return Files.newBufferedReader(p, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        }

        // Fallback a recurso en classpath
        InputStream is = getClass().getResourceAsStream("/auditoria/jacoco.csv");
        if (is != null) {
            log.info("Leyendo métricas JaCoCo desde classpath:/auditoria/jacoco.csv");
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        return null;
    }

    private void ejecutarMavenSiDisponible() {
        try {
            log.info("Iniciando ejecución de Maven para regenerar reporte JaCoCo...");
            String mavenCmd = "mvn";
            if (Files.isExecutable(Path.of("./mvnw"))) {
                mavenCmd = "./mvnw";
            } else if (Files.isExecutable(Path.of("PYF/backend/mvnw"))) {
                mavenCmd = "PYF/backend/mvnw";
            }
            ProcessBuilder pb = new ProcessBuilder(
                    mavenCmd, "test",
                    "-Dtest=!*PersistenciaTest,!*MigracionTest,!*ConcurrenteTest,!*PaginadoTest",
                    "jacoco:report"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean terminado = p.waitFor(90, TimeUnit.SECONDS);
            if (!terminado) {
                p.destroyForcibly();
                log.warn("Maven tardó más de 90 segundos y fue terminado");
            } else {
                log.info("Ejecución de Maven completada con código {}", p.exitValue());
            }
        } catch (Exception e) {
            log.warn("No fue posible ejecutar Maven en vivo: {}", e.getMessage());
        }
    }
}
