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

    /**
     * Filtros dermatológicos del catálogo (HU13): ids de los productos del catálogo aptos para el tipo
     * de piel elegido (sin irritantes, comedogénicos ni pH inadecuado para él) y, si se pide, sin
     * ingredientes de los alérgenos guardados en el perfil del usuario. Sin filtros devuelve todo el catálogo.
     *
     * @param tipoPiel código del tipo de piel (ej. "grasa"), o null para no filtrar por piel
     */
    List<Long> filtrarCatalogo(Long usuarioId, String tipoPiel, boolean excluirAlergenos);
}
