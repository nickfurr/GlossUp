package com.glossup.compatibilidad.domain.port.out;

import com.glossup.compatibilidad.domain.model.ReglaPerfil;

import java.util.List;

/**
 * Puerto de salida: catálogo de reglas de incompatibilidad producto–perfil (irritantes).
 */
public interface ReglaRepositoryPort {

    /** Todas las reglas ingrediente↔condición. La tabla es pequeña y se cruza en memoria. */
    List<ReglaPerfil> reglasPerfil();
}
