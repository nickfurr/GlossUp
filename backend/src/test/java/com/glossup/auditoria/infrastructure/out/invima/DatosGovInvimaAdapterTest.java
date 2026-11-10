package com.glossup.auditoria.infrastructure.out.invima;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.auditoria.domain.port.out.VerificacionInvimaPort;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DatosGovInvimaAdapter - Pruebas del adaptador de verificación INVIMA")
class DatosGovInvimaAdapterTest {

    private static Producto productoActivo(Long id, String reg) {
        return new Producto(
                id, 1L, "Producto " + id, "Marca", "FACIAL",
                Dinero.de(50000.0), 10, 5.5,
                reg, true, List.of(), List.of(), OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Debe retornar SIN_PRODUCTOS_ACTIVOS cuando el catálogo está vacío")
    void debeRetornarVacioSinProductos() {
        ProductoRepositoryPort repo = mock(ProductoRepositoryPort.class);
        when(repo.listarTodos()).thenReturn(List.of());

        DatosGovInvimaAdapter adapter = new DatosGovInvimaAdapter(repo, new ObjectMapper());
        VerificacionInvimaPort.ResultadoVerificacionInvima resultado = adapter.verificarProductosActivos();

        assertEquals("SIN_PRODUCTOS_ACTIVOS", resultado.fuente());
        assertEquals(0, resultado.totalProductos());
    }

    @Test
    @DisplayName("Debe verificar registros exitosamente cuando la API oficial responde")
    @SuppressWarnings("unchecked")
    void debeVerificarRegistrosConApiInvima() throws IOException, InterruptedException {
        ProductoRepositoryPort repo = mock(ProductoRepositoryPort.class);
        when(repo.listarTodos()).thenReturn(List.of(
                productoActivo(1L, "NSOC94562-19CO"),
                productoActivo(2L, "NSOC12345-20CO"),
                productoActivo(3L, "INVALID-CODE")
        ));

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                [
                  {
                    "registrosanitario": "NSOC94562-19CO",
                    "estadoregistro": "Vigente",
                    "titular": "L'OREAL CERAVE",
                    "fechavencimiento": "2029-12-31T00:00:00.000",
                    "grupo": "COSMETICOS"
                  }
                ]
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        DatosGovInvimaAdapter adapter = new DatosGovInvimaAdapter(repo, new ObjectMapper(), httpClient);
        VerificacionInvimaPort.ResultadoVerificacionInvima res = adapter.verificarProductosActivos();

        assertEquals(3, res.totalProductos());
        assertEquals(1, res.productosVigentes());
        assertEquals("DATOS_GOV_CO_INVIMA_UI32_P9F2", res.fuente());

        var p1 = res.detalles().stream().filter(d -> d.productoId().equals(1L)).findFirst().orElseThrow();
        assertTrue(p1.formatoNsocValido());
        assertEquals("Vigente", p1.estadoInvima());
        assertTrue(p1.verificadoEnLinea());

        var p3 = res.detalles().stream().filter(d -> d.productoId().equals(3L)).findFirst().orElseThrow();
        assertFalse(p3.formatoNsocValido());
        assertFalse(p3.verificadoEnLinea());
    }
}
