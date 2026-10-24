package com.glossup.catalogo.infrastructure.out.persistence.repository;

import com.glossup.catalogo.infrastructure.out.persistence.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    List<ProductoEntity> findByVendedorId(Long vendedorId);

    @Query("""
            SELECT p FROM ProductoEntity p WHERE p.activo = true AND (
                LOWER(p.nombre)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.marca)     LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.categoria) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    List<ProductoEntity> buscarPorPalabraClave(@Param("q") String q);
}

