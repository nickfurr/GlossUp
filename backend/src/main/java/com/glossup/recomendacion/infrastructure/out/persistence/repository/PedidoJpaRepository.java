package com.glossup.recomendacion.infrastructure.out.persistence.repository;

import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

    List<PedidoEntity> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    List<PedidoEntity> findByEstadoAndCreadoEnBefore(String estado, OffsetDateTime limite);
}
