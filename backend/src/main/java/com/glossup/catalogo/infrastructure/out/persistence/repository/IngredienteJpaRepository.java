package com.glossup.catalogo.infrastructure.out.persistence.repository;

import com.glossup.catalogo.infrastructure.out.persistence.entity.IngredienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredienteJpaRepository extends JpaRepository<IngredienteEntity, Long> {

    Optional<IngredienteEntity> findByNombreInciIgnoreCase(String nombreInci);

    List<IngredienteEntity> findTop20ByNombreInciContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrderByNombreInciAsc(
            String inci, String descripcion);
}

