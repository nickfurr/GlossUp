package com.glossup.usuarios.infrastructure.out.persistence.repository;

import com.glossup.usuarios.infrastructure.out.persistence.entity.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolJpaRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByNombre(String nombre);
}