package com.glossup.usuarios.domain.port.out;

/** Puerto de salida: datos personales del usuario que viven en otros módulos (perfil, direcciones). */
public interface DatosPersonalesPort {

    /** Borra el perfil dermatológico y la libreta de direcciones del usuario (Ley 1581 art. 8 e). */
    void suprimir(Long usuarioId);
}
