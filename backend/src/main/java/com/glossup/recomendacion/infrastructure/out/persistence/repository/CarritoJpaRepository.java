package com.glossup.recomendacion.infrastructure.out.persistence.repository;

import com.glossup.recomendacion.infrastructure.out.persistence.entity.CarritoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoJpaRepository extends JpaRepository<CarritoEntity, Long> {

    Optional<CarritoEntity> findByUsuarioId(Long usuarioId);
}
