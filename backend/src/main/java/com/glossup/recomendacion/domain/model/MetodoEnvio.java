package com.glossup.recomendacion.domain.model;

/** Formas de envío que el cliente elige en el checkout (HU14). */
public enum MetodoEnvio {
    ESTANDAR("Estándar"),
    EXPRESS("Express");

    private final String etiqueta;

    MetodoEnvio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() { return etiqueta; }
}
