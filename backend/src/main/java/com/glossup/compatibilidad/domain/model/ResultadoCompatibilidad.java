package com.glossup.compatibilidad.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una evaluación de compatibilidad (producto–perfil o producto–producto).
 *
 * <p>Regla de negocio: se considera "no compatible" si existe al menos una alerta de severidad
 * ALTA (alerta roja). Las alertas BAJA/MEDIA informan sin bloquear.</p>
 */
public class ResultadoCompatibilidad {

    private final List<Alerta> alertas;
    private final List<String> beneficios = new ArrayList<>();

    public ResultadoCompatibilidad(List<Alerta> alertas) {
        this.alertas = new ArrayList<>(alertas == null ? List.of() : alertas);
    }

    public static ResultadoCompatibilidad vacio() {
        return new ResultadoCompatibilidad(new ArrayList<>());
    }

    public void agregar(Alerta alerta) {
        if (alerta != null) alertas.add(alerta);
    }

    /** Por qué el producto le conviene al perfil (ingrediente recomendado para una de sus condiciones). */
    public void agregarBeneficio(String beneficio) {
        if (beneficio != null && !beneficios.contains(beneficio)) beneficios.add(beneficio);
    }

    /** Recomendado: sin ninguna alerta y con al menos un beneficio para el perfil. */
    public boolean esRecomendado() {
        return alertas.isEmpty() && !beneficios.isEmpty();
    }

    /** Compatible si no hay ninguna alerta crítica (ALTA). */
    public boolean esCompatible() {
        return alertas.stream().noneMatch(Alerta::esCritica);
    }

    public boolean tieneAlertas() {
        return !alertas.isEmpty();
    }

    public List<Alerta> getAlertas() {
        return List.copyOf(alertas);
    }

    public List<String> getBeneficios() {
        return List.copyOf(beneficios);
    }
}
