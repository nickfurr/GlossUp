package com.glossup.compatibilidad.domain.port.in;

import com.glossup.compatibilidad.domain.model.Cruce;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;

import java.util.List;
import java.util.Map;

/**
 * Puerto de entrada: compatibilidad producto–perfil (HU6, HU7) y producto–producto (HU8).
 */
public interface EvaluarCompatibilidadUseCase {

    /**
     * Evalúa si un producto es compatible con el perfil dermatológico de un usuario, cruzando
     * los ingredientes (INCI) del producto contra las alergias e irritantes del perfil.
     *
     * @throws com.glossup.shared.exception.NotFoundException si el producto no existe
     */
    ResultadoCompatibilidad evaluarProductoPerfil(Long usuarioId, Long productoId);

    /**
     * Evalúa varios productos de una vez contra el perfil (catálogo, carrito). Los ids que no
     * existen se omiten. Conserva el orden de entrada.
     */
    Map<Long, ResultadoCompatibilidad> evaluarProductosPerfil(Long usuarioId, List<Long> productoIds);

    /**
     * Cruces entre productos (HU8): cada par de productos distintos con un ingrediente de uno y otro
     * que no conviene combinar. No depende del perfil. Los ids que no existen se omiten.
     */
    List<Cruce> evaluarCruces(List<Long> productoIds);
}
