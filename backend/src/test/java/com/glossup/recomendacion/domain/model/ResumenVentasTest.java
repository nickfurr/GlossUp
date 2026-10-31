package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenVentasTest {

    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 4);

    private static VentaVendedor venta(String pagadoEn, PedidoItem... lineas) {
        Pedido pedido = new Pedido(1L, 1L, EstadoPedido.PAGADO, List.of(lineas), OffsetDateTime.parse(pagadoEn));
        pedido.conPago("t", "CARD", OffsetDateTime.parse(pagadoEn));
        return new VentaVendedor(pedido, "Ana", List.of(lineas));
    }

    private static PedidoItem linea(long id, int cantidad, double precio) {
        return new PedidoItem(id, "Producto " + id, cantidad, Dinero.de(precio));
    }

    @Test
    void sinVentas_daCerosYOchoSemanasTerminandoEnLaActual() {
        ResumenVentas r = ResumenVentas.de(List.of(), MIERCOLES);
        assertEquals(Dinero.cero(), r.ingresosMes());
        assertEquals(0, r.pedidosMes());
        assertEquals(8, r.semanas().size());
        assertEquals(LocalDate.of(2026, 11, 2), r.semanas().get(7).inicio(), "la semana empieza el lunes");
        assertEquals(LocalDate.of(2026, 9, 14), r.semanas().get(0).inicio());
    }

    @Test
    void elMesSoloCuentaLoPagadoEnElMesActual() {
        ResumenVentas r = ResumenVentas.de(List.of(
                venta("2026-11-03T15:00:00Z", linea(1, 2, 10000)),
                venta("2026-11-01T15:00:00Z", linea(2, 1, 5000)),
                venta("2026-10-15T15:00:00Z", linea(1, 1, 10000))), MIERCOLES);
        assertEquals(Dinero.de(25000), r.ingresosMes());
        assertEquals(2, r.pedidosMes());
        assertEquals(3, r.unidadesMes());
    }

    @Test
    void lasFechasSeCuentanEnHoraDeColombia() {
        // 2 de noviembre 03:00 UTC es todavía 1 de noviembre 22:00 en Colombia: semana anterior, mismo mes.
        ResumenVentas r = ResumenVentas.de(List.of(venta("2026-11-02T03:00:00Z", linea(1, 1, 10000))), MIERCOLES);
        assertEquals(Dinero.cero(), r.semanas().get(7).ingresos());
        assertEquals(Dinero.de(10000), r.semanas().get(6).ingresos());
        // 1 de noviembre 03:00 UTC es 31 de octubre en Colombia: no entra al mes.
        assertEquals(Dinero.cero(),
                ResumenVentas.de(List.of(venta("2026-11-01T03:00:00Z", linea(1, 1, 10000))), MIERCOLES).ingresosMes());
    }

    @Test
    void ventasMasViejasQueOchoSemanasNoRompenLaGrafica() {
        ResumenVentas r = ResumenVentas.de(List.of(venta("2026-01-05T15:00:00Z", linea(1, 1, 10000))), MIERCOLES);
        assertEquals(8, r.semanas().size());
        r.semanas().forEach(s -> assertEquals(Dinero.cero(), s.ingresos()));
    }

    @Test
    void losMasVendidosSumanPorProductoYSeOrdenanPorIngresos() {
        ResumenVentas r = ResumenVentas.de(List.of(
                venta("2026-11-03T15:00:00Z", linea(1, 1, 10000), linea(2, 1, 50000)),
                venta("2026-11-02T15:00:00Z", linea(1, 2, 10000))), MIERCOLES);
        assertEquals(2L, r.masVendidos().get(0).productoId());
        assertEquals(1L, r.masVendidos().get(1).productoId());
        assertEquals(3, r.masVendidos().get(1).unidades());
        assertEquals(Dinero.de(30000), r.masVendidos().get(1).ingresos());
    }
}
