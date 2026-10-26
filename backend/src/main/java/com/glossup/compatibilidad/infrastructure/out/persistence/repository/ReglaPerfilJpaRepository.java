package com.glossup.compatibilidad.infrastructure.out.persistence.repository;

import com.glossup.compatibilidad.infrastructure.out.persistence.entity.ReglaPerfilEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReglaPerfilJpaRepository extends JpaRepository<ReglaPerfilEntity, Long> {
}
