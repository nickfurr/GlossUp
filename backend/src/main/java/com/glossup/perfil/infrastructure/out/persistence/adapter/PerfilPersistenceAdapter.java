package com.glossup.perfil.infrastructure.out.persistence.adapter;

import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import com.glossup.perfil.infrastructure.out.persistence.entity.PerfilAfeccionEntity;
import com.glossup.perfil.infrastructure.out.persistence.entity.PerfilAlergiaEntity;
import com.glossup.perfil.infrastructure.out.persistence.entity.PerfilDermatologicoEntity;
import com.glossup.perfil.infrastructure.out.persistence.repository.PerfilJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PerfilPersistenceAdapter implements PerfilRepositoryPort {

    private final PerfilJpaRepository perfilJpaRepository;

    public PerfilPersistenceAdapter(PerfilJpaRepository perfilJpaRepository) {
        this.perfilJpaRepository = perfilJpaRepository;
    }

    @Override
    public Optional<PerfilDermatologico> buscarPorUsuarioId(Long usuarioId) {
        return perfilJpaRepository.findByUsuarioId(usuarioId).map(this::aDominio);
    }

    @Override
    public PerfilDermatologico guardar(PerfilDermatologico dominio) {
        PerfilDermatologicoEntity entity = perfilJpaRepository.findByUsuarioId(dominio.getUsuarioId())
                .orElseGet(PerfilDermatologicoEntity::new);

        if (entity.getId() == null && dominio.getId() != null) {
            entity.setId(dominio.getId());
        }
        entity.setUsuarioId(dominio.getUsuarioId());
        entity.setPh(dominio.getPh() != null
                ? BigDecimal.valueOf(dominio.getPh().valor()).setScale(1, RoundingMode.HALF_UP)
                : null);
        entity.setTipoPiel(dominio.getTipoPiel() != null ? dominio.getTipoPiel().name() : null);
        entity.setTipoCabello(dominio.getTipoCabello() != null ? dominio.getTipoCabello().name() : null);
        entity.setActualizadoEn(dominio.getActualizadoEn());

        // Sincronizar afecciones
        entity.getAfecciones().clear();
        for (Afeccion afeccion : dominio.getAfecciones()) {
            entity.agregarAfeccion(new PerfilAfeccionEntity(entity, afeccion.name()));
        }

        // Sincronizar alergias
        entity.getAlergias().clear();
        for (Alergeno alergia : dominio.getAlergias()) {
            entity.agregarAlergia(new PerfilAlergiaEntity(entity, alergia.name()));
        }

        PerfilDermatologicoEntity guardado = perfilJpaRepository.save(entity);
        return aDominio(guardado);
    }

    private PerfilDermatologico aDominio(PerfilDermatologicoEntity entity) {
        Ph ph = entity.getPh() != null ? new Ph(entity.getPh().doubleValue()) : null;
        TipoPiel tipoPiel = entity.getTipoPiel() != null ? TipoPiel.valueOf(entity.getTipoPiel()) : null;

        TipoCabello tipoCabello = entity.getTipoCabello() != null ? TipoCabello.valueOf(entity.getTipoCabello()) : null;

        Set<Afeccion> afecciones = entity.getAfecciones().stream()
                .map(a -> Afeccion.valueOf(a.getNombre()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Alergeno> alergias = entity.getAlergias().stream()
                .map(a -> Alergeno.valueOf(a.getNombre()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PerfilDermatologico(
                entity.getId(),
                entity.getUsuarioId(),
                ph,
                tipoPiel,
                tipoCabello,
                afecciones,
                alergias,
                entity.getActualizadoEn()
        );
    }
}
