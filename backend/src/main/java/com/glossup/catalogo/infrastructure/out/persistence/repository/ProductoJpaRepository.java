package com.glossup.catalogo.infrastructure.out.persistence.repository;

import com.glossup.catalogo.infrastructure.out.persistence.entity.ProductoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    List<ProductoEntity> findByVendedorId(Long vendedorId);

    /** Catálogo publicado (activo, sin bloqueo y con stock, como {@code Producto.estaEnCatalogo}) por páginas (auditoría R-14). */
    @Query(value = "SELECT p FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 ORDER BY p.id",
           countQuery = "SELECT count(p) FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0")
    Page<ProductoEntity> catalogo(Pageable pagina);

    @Query(value = "SELECT p FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 "
            + "AND p.categoria = :categoria ORDER BY p.id",
           countQuery = "SELECT count(p) FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 "
            + "AND p.categoria = :categoria")
    Page<ProductoEntity> catalogoPorCategoria(@Param("categoria") String categoria, Pageable pagina);

    /** Solo id y categoría del catálogo publicado: lo que el navegador necesita para contar y filtrar sin bajar fichas. */
    @Query("SELECT p.id, p.categoria FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 ORDER BY p.id")
    List<Object[]> indiceCatalogo();

    /**
     * Suma {@code delta} al stock en una sola sentencia: PostgreSQL bloquea la fila hasta el commit, así
     * que dos compras simultáneas se esperan en vez de chocar, y el stock nunca baja de 0 (auditoría R-05).
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductoEntity p SET p.stock = p.stock + :delta WHERE p.id = :id AND p.stock + :delta >= 0")
    int ajustarStock(@Param("id") Long id, @Param("delta") int delta);

    /**
     * {@code q} debe llegar en minúsculas y sin tildes: la comparación ignora tildes ("serum" encuentra "Sérum").
     * Solo productos en catálogo: activos, sin bloqueo y con stock (mismo criterio que {@code Producto.estaEnCatalogo}).
     * También encuentra por ingrediente: nombre INCI ("niacinamide") o nombre común ("niacinamida") (auditoría OBS-F02).
     */
    @Query("""
            SELECT p FROM ProductoEntity p WHERE p.activo = true AND p.baneado = false AND p.stock > 0 AND (
                cast(function('translate', LOWER(p.nombre),    'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%') OR
                cast(function('translate', LOWER(p.marca),     'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%') OR
                cast(function('translate', LOWER(p.categoria), 'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%') OR
                EXISTS (SELECT 1 FROM ProductoIngredienteEntity pi WHERE pi.producto = p AND (
                    LOWER(pi.ingrediente.nombreInci) LIKE CONCAT('%', :q, '%') OR
                    cast(function('translate', LOWER(pi.ingrediente.descripcion), 'áéíóúüñ', 'aeiouun') as String) LIKE CONCAT('%', :q, '%'))))
            """)
    List<ProductoEntity> buscarPorPalabraClave(@Param("q") String q);
}

