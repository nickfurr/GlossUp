package com.glossup.perfil.infrastructure.in.rest.dto;

import com.glossup.perfil.domain.model.PerfilDermatologico;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO de salida que representa el estado actual del perfil dermatológico del usuario.
 */
public record PerfilResponse(
        Long id,
        Long usuarioId,
        Double ph,
        String tipoPiel,
        String tipoCabello,
        Set<String> afecciones,
        Set<String> alergias,
        OffsetDateTime actualizadoEn
) {
    public static PerfilResponse fromDomain(PerfilDermatologico perfil) {
        return new PerfilResponse(
                perfil.getId(),
                perfil.getUsuarioId(),
                perfil.getPh() != null ? perfil.getPh().valor() : null,
                perfil.getTipoPiel() != null ? perfil.getTipoPiel().name() : null,
                perfil.getTipoCabello() != null ? perfil.getTipoCabello().name() : null,
                codigos(perfil.getAfecciones()),
                codigos(perfil.getAlergias()),
                perfil.getActualizadoEn()
        );
    }

    private static Set<String> codigos(Set<? extends Enum<?>> valores) {
        return valores.stream().map(Enum::name).collect(Collectors.toSet());
    }
}
