package com.glossup.perfil.infrastructure.out.persistence.adapter;

import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
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
        entity.setTipoCabello(dominio.getTipoCabello());
        entity.setActualizadoEn(dominio.getActualizadoEn());

        // Sincronizar afecciones
        entity.getAfecciones().clear();
        for (String afeccion : dominio.getAfecciones()) {
            entity.agregarAfeccion(new PerfilAfeccionEntity(entity, afeccion));
        }

        // Sincronizar alergias
        entity.getAlergias().clear();
        for (String alergia : dominio.getAlergias()) {
            entity.agregarAlergia(new PerfilAlergiaEntity(entity, alergia));
        }

        PerfilDermatologicoEntity guardado = perfilJpaRepository.save(entity);
        return aDominio(guardado);
    }

    private PerfilDermatologico aDominio(PerfilDermatologicoEntity entity) {
        Ph ph = entity.getPh() != null ? new Ph(entity.getPh().doubleValue()) : null;
        TipoPiel tipoPiel = entity.getTipoPiel() != null ? TipoPiel.valueOf(entity.getTipoPiel()) : null;

        Set<String> afecciones = entity.getAfecciones().stream()
                .map(PerfilAfeccionEntity::getNombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> alergias = entity.getAlergias().stream()
                .map(PerfilAlergiaEntity::getNombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PerfilDermatologico(
                entity.getId(),
                entity.getUsuarioId(),
                ph,
                tipoPiel,
                entity.getTipoCabello(),
                afecciones,
                alergias,
                entity.getActualizadoEn()
        );
    }
}
