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
     * Agrega un alérgeno del catálogo cerrado (código de {@code Alergeno}) al perfil (HU2).
     */
    PerfilDermatologico agregarAlergia(Long usuarioId, String alergia);

    /**
     * Quita un alérgeno (código de {@code Alergeno}) del perfil (HU2).
     */
    PerfilDermatologico quitarAlergia(Long usuarioId, String alergia);

    /**
     * Agrega una afección del catálogo cerrado (código de {@code Afeccion}) al perfil (HU2).
     */
    PerfilDermatologico agregarAfeccion(Long usuarioId, String afeccion);

    /**
     * Quita una afección (código de {@code Afeccion}) del perfil (HU2).
     */
    PerfilDermatologico quitarAfeccion(Long usuarioId, String afeccion);
}
