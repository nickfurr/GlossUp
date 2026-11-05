package com.glossup.recomendacion.infrastructure.out.compatibilidad;

import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.recomendacion.domain.model.MotorRecomendacion;
import com.glossup.recomendacion.domain.port.out.EvaluacionPerfilPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Une el perfil dermatológico y el motor de compatibilidad para el recomendador. */
@Component
public class EvaluacionPerfilAdapter implements EvaluacionPerfilPort {

    private final GestionarPerfilUseCase perfiles;
    private final EvaluarCompatibilidadUseCase compatibilidad;

    public EvaluacionPerfilAdapter(GestionarPerfilUseCase perfiles, EvaluarCompatibilidadUseCase compatibilidad) {
        this.perfiles = perfiles;
        this.compatibilidad = compatibilidad;
    }

    @Override
    public Evaluacion evaluar(Long usuarioId, List<Long> productoIds) {
        PerfilDermatologico perfil = perfiles.obtenerPerfil(usuarioId);
        // Mismo criterio que la pantalla del catálogo: sin piel, alergias ni afecciones no hay contra qué cruzar.
        if (perfil.getTipoPiel() == null && perfil.getAlergias().isEmpty() && perfil.getAfecciones().isEmpty()) {
            return new Evaluacion(true, Map.of());
        }
        Map<Long, MotorRecomendacion.Calificacion> calificaciones = new HashMap<>();
        compatibilidad.evaluarProductosPerfil(usuarioId, productoIds).forEach((id, r) -> calificaciones.put(id,
                new MotorRecomendacion.Calificacion(r.esCompatible(), r.tieneAlertas(), r.getBeneficios())));
        return new Evaluacion(false, calificaciones);
    }
}
