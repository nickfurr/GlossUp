package com.glossup.recomendacion.infrastructure.out.persistence.repository;

import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {
}
