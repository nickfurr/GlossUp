package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cifras del panel del vendedor. Las fechas se cuentan en hora de Colombia, no en la del servidor. */
public record ResumenVentas(Dinero ingresosMes, int pedidosMes, int unidadesMes, List<Semana> semanas,
                            List<MasVendido> masVendidos) {

    public static final ZoneId HORA_COLOMBIA = ZoneId.of("America/Bogota");
    private static final int SEMANAS = 8;
    private static final int MAS_VENDIDOS = 5;

    public record Semana(LocalDate inicio, Dinero ingresos) {}

    public record MasVendido(Long productoId, String nombre, int unidades, Dinero ingresos) {}

    public static ResumenVentas de(List<VentaVendedor> ventas, LocalDate hoy) {
        YearMonth mes = YearMonth.from(hoy);
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Map<LocalDate, Dinero> porSemana = new LinkedHashMap<>();
        for (int i = SEMANAS - 1; i >= 0; i--) {
            porSemana.put(lunes.minusWeeks(i), Dinero.cero());
        }

        Dinero ingresosMes = Dinero.cero();
        int pedidosMes = 0;
        int unidadesMes = 0;
        Map<Long, MasVendido> porProducto = new LinkedHashMap<>();
        for (VentaVendedor venta : ventas) {
            LocalDate dia = venta.pedido().getPagadoEn().atZoneSameInstant(HORA_COLOMBIA).toLocalDate();
            if (YearMonth.from(dia).equals(mes)) {
                ingresosMes = ingresosMes.mas(venta.ingresos());
                pedidosMes++;
                unidadesMes += venta.unidades();
            }
            porSemana.computeIfPresent(dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    (semana, total) -> total.mas(venta.ingresos()));
            for (PedidoItem l : venta.lineas()) {
                porProducto.merge(l.productoId(), new MasVendido(l.productoId(), l.nombre(), l.cantidad(), l.subtotal()),
                        (a, b) -> new MasVendido(a.productoId(), a.nombre(), a.unidades() + b.unidades(), a.ingresos().mas(b.ingresos())));
            }
        }

        List<Semana> semanas = new ArrayList<>();
        porSemana.forEach((inicio, total) -> semanas.add(new Semana(inicio, total)));
        List<MasVendido> masVendidos = porProducto.values().stream()
                .sorted(Comparator.comparing((MasVendido m) -> m.ingresos().monto()).reversed())
                .limit(MAS_VENDIDOS)
                .toList();
        return new ResumenVentas(ingresosMes, pedidosMes, unidadesMes, semanas, masVendidos);
    }
}
