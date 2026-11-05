package com.glossup.compatibilidad.domain.model;

/**
 * Ingrediente (INCI, minúsculas) recomendado para una condición del perfil, con su motivo. {@code zona} null vale
 * para cualquier producto; si no, solo para los de esa zona (p. ej. los del tipo de cabello, solo en capilares).
 */
public record Beneficio(String ingrediente, String condicion, String motivo, Zona zona) {

    public Beneficio(String ingrediente, String condicion, String motivo) {
        this(ingrediente, condicion, motivo, null);
    }

    public boolean aplicaEn(Zona zonaProducto) {
        return zona == null || zona == zonaProducto;
    }
}
