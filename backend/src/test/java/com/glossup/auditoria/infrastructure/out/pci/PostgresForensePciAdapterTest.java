package com.glossup.auditoria.infrastructure.out.pci;

import com.glossup.auditoria.domain.port.out.AuditoriaForensePciPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("PostgresForensePciAdapter - Pruebas de auditoría forense PCI-DSS")
class PostgresForensePciAdapterTest {

    @Test
    @DisplayName("Debe auditar correctamente sin hallar columnas prohibidas")
    void debeAuditarSinColumnasProhibidas() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(jdbcTemplate.queryForList(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("information_schema.columns")) {
                return List.of(
                        Map.of("table_name", "pedido", "column_name", "id"),
                        Map.of("table_name", "pedido", "column_name", "pago_transaccion_id"),
                        Map.of("table_name", "pedido", "column_name", "total")
                );
            }
            if (sql.contains("FROM pedido")) {
                return List.of(
                        Map.of("id", 1L, "transaccion", "tx_1234567890abcdef")
                );
            }
            return List.of();
        });

        PostgresForensePciAdapter adapter = new PostgresForensePciAdapter(jdbcTemplate);
        AuditoriaForensePciPort.ResultadoAuditoriaPci resultado = adapter.ejecutarAuditoria();

        assertNotNull(resultado);
        assertTrue(resultado.columnasProhibidasDetectadas().isEmpty());
        assertEquals(0, resultado.registrosConPatronesPan());
        assertEquals(3, resultado.totalColumnas());
        assertEquals(1, resultado.totalTablas());
    }

    @Test
    @DisplayName("Debe detectar columna prohibida si existe en information_schema")
    void debeDetectarColumnaProhibida() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(jdbcTemplate.queryForList(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("information_schema.columns")) {
                return List.of(
                        Map.of("table_name", "tarjetas", "column_name", "cvv")
                );
            }
            return List.of();
        });

        PostgresForensePciAdapter adapter = new PostgresForensePciAdapter(jdbcTemplate);
        AuditoriaForensePciPort.ResultadoAuditoriaPci resultado = adapter.ejecutarAuditoria();

        assertFalse(resultado.columnasProhibidasDetectadas().isEmpty());
        assertTrue(resultado.columnasProhibidasDetectadas().contains("tarjetas.cvv"));
    }
}
