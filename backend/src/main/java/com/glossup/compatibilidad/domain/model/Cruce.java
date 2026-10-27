package com.glossup.compatibilidad.domain.model;

/** Choque entre dos productos del carrito (HU8): un ingrediente de cada uno que no conviene combinar. */
public record Cruce(Long productoA, Long productoB, Alerta alerta) {}
