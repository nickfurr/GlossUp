package com.glossup.usuarios.domain.port.out;

import java.time.OffsetDateTime;
import java.util.Optional;

/** Puerto de salida: enlaces de recuperación de contraseña. Solo conoce el hash del token. */
public interface RecuperacionClaveRepositoryPort {

    /** Guarda el enlace y anula los anteriores del usuario: solo vale el último. */
    void guardar(String tokenHash, Long usuarioId, OffsetDateTime expiraEn);

    boolean hayRecienteDe(Long usuarioId, OffsetDateTime desde);

    /** Si el enlace existe y no venció, lo borra y devuelve su usuario. Atómico: un enlace no se usa dos veces. */
    Optional<Long> consumir(String tokenHash);
}
