package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.MotorRecomendacion;

import java.util.List;
import java.util.Map;

/** Puerto de salida: lo que dicen el perfil dermatológico y el motor de compatibilidad sobre unos productos. */
public interface EvaluacionPerfilPort {

    Evaluacion evaluar(Long usuarioId, List<Long> productoIds);

    /** {@code perfilVacio}: el cliente no ha dicho su tipo de piel, alergias ni afecciones. */
    record Evaluacion(boolean perfilVacio, Map<Long, MotorRecomendacion.Calificacion> calificaciones) {}
}
