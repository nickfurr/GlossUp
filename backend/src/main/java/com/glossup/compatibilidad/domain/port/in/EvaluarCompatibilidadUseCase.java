package com.glossup.compatibilidad.domain.port.in;

import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;

import java.util.List;
import java.util.Map;

/**
 * Puerto de entrada: evaluación de compatibilidad producto–perfil (HU6).
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
}
