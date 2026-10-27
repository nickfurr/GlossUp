package com.glossup.compatibilidad.domain.model;

/** Ingrediente (INCI, minúsculas) recomendado para una condición del perfil, con su motivo. */
public record Beneficio(String ingrediente, String condicion, String motivo) {}
