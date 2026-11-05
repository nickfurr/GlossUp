package com.glossup.recomendacion.domain.port.out;

import com.glossup.recomendacion.domain.model.MotorRecomendacion;

import java.util.List;

/** Puerto de salida: productos publicados y con stock, con sus ingredientes, que se pueden recomendar. */
public interface CatalogoRecomendablePort {

    List<MotorRecomendacion.Candidato> disponibles();
}
