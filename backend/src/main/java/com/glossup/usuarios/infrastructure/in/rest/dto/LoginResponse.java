package com.glossup.usuarios.infrastructure.in.rest.dto;

import java.util.Set;

/** Datos para mostrar la sesión. El token no va aquí: viaja en la cookie HttpOnly (auditoría OBS-T09). */
public record LoginResponse(Long usuarioId, String email, String nombre, Set<String> roles) {}
