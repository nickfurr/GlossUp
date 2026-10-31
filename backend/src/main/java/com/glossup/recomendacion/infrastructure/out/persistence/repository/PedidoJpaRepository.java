package com.glossup.recomendacion.infrastructure.out.persistence.repository;

import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

    List<PedidoEntity> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    List<PedidoEntity> findByEstadoAndCreadoEnBefore(String estado, OffsetDateTime limite);

    List<PedidoEntity> findByEstadoAndEnvioEstadoAndEnvioIntentosLessThanAndPagadoEnBefore(
            String estado, String envioEstado, Integer maxIntentos, OffsetDateTime limite);

    List<PedidoEntity> findByEnvioEstadoIn(List<String> estados);

    Optional<PedidoEntity> findByEnvioGuia(String guia);

    List<PedidoEntity> findAllByOrderByCreadoEnDesc();

    @Query("""
            select distinct p from PedidoEntity p join p.items i
            where i.productoId in :productoIds and p.estado in ('PAGADO', 'ENVIADO')
            order by p.pagadoEn desc
            """)
    List<PedidoEntity> buscarPagadosConProductos(Set<Long> productoIds);
}
