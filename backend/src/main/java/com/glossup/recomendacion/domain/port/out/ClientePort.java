package com.glossup.recomendacion.domain.port.out;

import java.util.Optional;

/** Puerto de salida: datos del cliente que van en el comprobante y a quién se le envía (HU11). */
public interface ClientePort {

    Optional<Cliente> buscar(Long usuarioId);

    record Cliente(String nombre, String email) {}
}
