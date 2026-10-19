package com.glossup.usuarios.infrastructure.in.rest.dto;

import java.util.Set;

public record LoginResponse(String token, Long usuarioId, String email, String nombre, Set<String> roles) {}
