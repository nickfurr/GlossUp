package com.glossup.compatibilidad.domain.model;

/**
 * Regla por rango (HU7): si un valor del producto cae en [desde, hasta] y el perfil tiene la
 * condición, se alerta. El atributo es el grado comedogénico de un ingrediente o el pH del producto.
 */
public record ReglaRango(Atributo atributo, String condicion, double desde, double hasta,
                         Severidad severidad, String motivo) {

    public enum Atributo { COMEDOGENICIDAD, PH }

    public boolean contiene(double valor) {
        return valor >= desde && valor <= hasta;
    }
}
