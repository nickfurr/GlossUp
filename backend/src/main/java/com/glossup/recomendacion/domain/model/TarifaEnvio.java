package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Costo y plazo de envío por zona (HU14). El servidor siempre calcula el costo: el cliente solo
 * elige la forma de envío. GlossUP despacha desde Quindío.
 */
public final class TarifaEnvio {

    public static final List<String> DEPARTAMENTOS = List.of(
            "Amazonas", "Antioquia", "Arauca", "Atlántico", "Bogotá D.C.", "Bolívar", "Boyacá", "Caldas", "Caquetá",
            "Casanare", "Cauca", "Cesar", "Chocó", "Córdoba", "Cundinamarca", "Guainía", "Guaviare", "Huila",
            "La Guajira", "Magdalena", "Meta", "Nariño", "Norte de Santander", "Putumayo", "Quindío", "Risaralda",
            "San Andrés y Providencia", "Santander", "Sucre", "Tolima", "Valle del Cauca", "Vaupés", "Vichada");

    private static final String LOCAL = "Quindío";
    private static final Set<String> DIFICIL_ACCESO = Set.of(
            "Amazonas", "Guainía", "Guaviare", "Putumayo", "San Andrés y Providencia", "Vaupés", "Vichada");
    private static final Dinero ENVIO_GRATIS_DESDE = Dinero.de(150000);

    private TarifaEnvio() {}

    public record Opcion(MetodoEnvio metodo, Dinero costo, int diasHabiles) {}

    /** Formas de envío disponibles para el departamento, de la más económica a la más rápida. */
    public static List<Opcion> opciones(String departamento, Dinero subtotal) {
        if (!DEPARTAMENTOS.contains(departamento)) {
            throw new ReglaNegocioException("Departamento no válido: " + departamento);
        }
        if (DIFICIL_ACCESO.contains(departamento)) {
            return List.of(new Opcion(MetodoEnvio.ESTANDAR, Dinero.de(28000), 8));
        }
        boolean gratis = subtotal.monto().compareTo(ENVIO_GRATIS_DESDE.monto()) >= 0;
        return LOCAL.equals(departamento)
                ? List.of(new Opcion(MetodoEnvio.ESTANDAR, gratis ? Dinero.cero() : Dinero.de(6000), 2),
                          new Opcion(MetodoEnvio.EXPRESS, Dinero.de(10000), 1))
                : List.of(new Opcion(MetodoEnvio.ESTANDAR, gratis ? Dinero.cero() : Dinero.de(12000), 4),
                          new Opcion(MetodoEnvio.EXPRESS, Dinero.de(22000), 2));
    }

    /** shortcut: salta fines de semana pero no festivos; usar el calendario de la transportadora cuando haya una real. */
    public static LocalDate fechaEstimada(LocalDate desde, int diasHabiles) {
        LocalDate fecha = desde;
        for (int faltan = diasHabiles; faltan > 0; ) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                faltan--;
            }
        }
        return fecha;
    }
}
