package com.glossup.usuarios.domain.port.in;

public record RegistrarUsuarioCommand(
        String email,
        String nombre,
        String password,
        String rol,
        boolean aceptaPolitica,
        boolean mayorDeEdad
) {}
