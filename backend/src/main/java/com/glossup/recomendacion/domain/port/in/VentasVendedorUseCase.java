package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.ResumenVentas;
import com.glossup.recomendacion.domain.model.VentaVendedor;

import java.util.List;

/** Puerto de entrada: pedidos pagados con productos del vendedor y sus cifras de venta. */
public interface VentasVendedorUseCase {

    Ventas ventas(Long vendedorId);

    record Ventas(ResumenVentas resumen, List<VentaVendedor> ventas) {}
}
