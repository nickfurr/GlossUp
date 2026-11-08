package com.glossup.recomendacion.domain.port.in;

import com.glossup.recomendacion.domain.model.Recomendacion;

import java.util.List;

/** Puerto de entrada: recomendaciones personalizadas para el cliente (auditoría NC-T10). */
public interface RecomendarProductosUseCase {

    /** Los mejores productos para el cliente, del más al menos afín, cada uno con su razón. */
    ResultadoRecomendacion recomendar(Long usuarioId, int limite);

    /** {@code perfilVacio}: sin perfil dermatológico no hay reglas que aplicar y se recomienda por lo demás. */
    record ResultadoRecomendacion(boolean perfilVacio, List<Recomendacion> recomendaciones) {}
}
