package com.glossup.perfil.domain.port.in;

import com.glossup.perfil.domain.model.PerfilDermatologico;

/**
 * Puerto de entrada para la consulta y gestión dinámica del perfil dermatológico (HU2 - SCRUM-29).
 */
public interface GestionarPerfilUseCase {

    /**
     * Obtiene el perfil dermatológico del usuario autenticado.
     * Si el usuario aún no posee un perfil registrado, crea y persiste uno inicial vacío.
     */
    PerfilDermatologico obtenerPerfil(Long usuarioId);

    PerfilDermatologico actualizarBiotipo(Long usuarioId, Double ph, String tipoPiel, String tipoCabello);

    /**
     * Agrega dinámicamente un ingrediente (INCI) a la lista de alergias del usuario (HU2).
     */
    PerfilDermatologico agregarAlergia(Long usuarioId, String alergia);

    /**
     * Elimina dinámicamente un ingrediente (INCI) de la lista de alergias del usuario (HU2).
     */
    PerfilDermatologico quitarAlergia(Long usuarioId, String alergia);

    /**
     * Agrega dinámicamente una condición o afección dermatológica al perfil del usuario (HU2).
     */
    PerfilDermatologico agregarAfeccion(Long usuarioId, String afeccion);

    /**
     * Elimina dinámicamente una condición o afección dermatológica del perfil del usuario (HU2).
     */
    PerfilDermatologico quitarAfeccion(Long usuarioId, String afeccion);
}
