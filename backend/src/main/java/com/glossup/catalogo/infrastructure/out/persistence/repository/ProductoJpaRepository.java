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

    /**
     * {@code q} debe llegar en minúsculas y sin tildes: la comparación ignora tildes ("serum" encuentra "Sérum").
     * Solo productos en catálogo: activos, sin bloqueo y con stock (mismo criterio que {@code Producto.estaEnCatalogo}).
     */
    @Query("""
            SELECT p FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 AND (
                cast(function('translate', LOWER(p.nombre),    'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%') OR
                cast(function('translate', LOWER(p.marca),     'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%') OR
                cast(function('translate', LOWER(p.categoria), 'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%'))
            """)
    List<ProductoEntity> buscarPorPalabraClave(@Param("q") String q);
}

