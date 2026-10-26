package com.glossup.compatibilidad.infrastructure.out.persistence.adapter;

import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import com.glossup.compatibilidad.infrastructure.out.persistence.entity.ReglaPerfilEntity;
import com.glossup.compatibilidad.infrastructure.out.persistence.repository.ReglaPerfilJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglaPerfilPersistenceAdapter implements ReglaRepositoryPort {

    private final ReglaPerfilJpaRepository jpa;

    public ReglaPerfilPersistenceAdapter(ReglaPerfilJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ReglaPerfil> reglasPerfil() {
        return jpa.findAll().stream().map(this::aDominio).toList();
    }

    private ReglaPerfil aDominio(ReglaPerfilEntity e) {
        return new ReglaPerfil(e.getId(), e.getIngredienteInci(), e.getCondicion(),
                Severidad.valueOf(e.getSeveridad()), e.getMotivo());
    }
}
