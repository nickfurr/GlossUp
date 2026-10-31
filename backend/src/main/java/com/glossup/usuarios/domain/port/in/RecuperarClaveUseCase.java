package com.glossup.usuarios.domain.port.in;

/** Puerto de entrada: recuperar la contraseña con un enlace de un solo uso. */
public interface RecuperarClaveUseCase {

    /** El usuario lo pide con su correo. No revela si el correo existe. */
    void solicitar(String email);

    /** El administrador genera el enlace para entregárselo al usuario. Nunca ve ni fija la contraseña. */
    String generarEnlace(Long usuarioId);

    void restablecer(String token, String claveNueva);
}
