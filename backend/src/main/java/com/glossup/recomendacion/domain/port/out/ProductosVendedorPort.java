package com.glossup.recomendacion.domain.port.out;

import java.util.Set;

/** Puerto de salida: qué productos del catálogo son de un vendedor. */
public interface ProductosVendedorPort {

    Set<Long> idsDe(Long vendedorId);
}
