package com.glossup.perfil.infrastructure.out.persistence.repository;

import com.glossup.perfil.infrastructure.out.persistence.entity.PerfilDermatologicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilJpaRepository extends JpaRepository<PerfilDermatologicoEntity, Long> {

    Optional<PerfilDermatologicoEntity> findByUsuarioId(Long usuarioId);
}
