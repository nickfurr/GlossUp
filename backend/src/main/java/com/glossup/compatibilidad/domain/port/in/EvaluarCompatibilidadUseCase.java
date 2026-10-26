package com.glossup.compatibilidad.domain.port.in;

import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;

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
}
