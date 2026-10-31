package com.glossup.recomendacion.domain.model;

/** Dirección de entrega de la libreta del cliente; también es la copia que queda en el envío del pedido (HU14). */
public record Direccion(
        Long id,
        Long usuarioId,
        String nombreRecibe,
        String telefono,
        String departamento,
        String ciudad,
        String barrio,
        String direccion,
        String complemento,
        String indicaciones,
        boolean principal
) {
    public Direccion conId(Long nuevoId, boolean esPrincipal) {
        return new Direccion(nuevoId, usuarioId, nombreRecibe, telefono, departamento, ciudad, barrio, direccion,
                complemento, indicaciones, esPrincipal);
    }
}
