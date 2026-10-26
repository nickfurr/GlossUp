package com.glossup.compatibilidad.domain.port.out;

import java.util.Set;

/**
 * Puerto de salida: datos del perfil dermatológico que el motor necesita, sin acoplarse al
 * agregado {@code PerfilDermatologico} del módulo de perfil.
 */
public interface PerfilConsultaPort {

    /** Perfil de un usuario. Si no tiene perfil, ambos conjuntos vienen vacíos. */
    PerfilConsulta obtener(Long usuarioId);

    /**
     * @param alergias   ingredientes (INCI) a los que el usuario declaró alergia, normalizados
     * @param condiciones afecciones + tipo de piel del usuario, normalizados (para cruzar irritantes)
     */
    record PerfilConsulta(Set<String> alergias, Set<String> condiciones) {}
}
