package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoItemEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.repository.PedidoJpaRepository;
import com.glossup.shared.domain.Dinero;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class PedidoPersistenceAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository pedidoJpaRepository;

    public PedidoPersistenceAdapter(PedidoJpaRepository pedidoJpaRepository) {
        this.pedidoJpaRepository = pedidoJpaRepository;
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoEntity entity = aEntity(pedido);
        PedidoEntity guardado = pedidoJpaRepository.save(entity);
        return aDominio(guardado);
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoJpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public List<Pedido> buscarPorUsuario(Long usuarioId) {
        return pedidoJpaRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).stream().map(this::aDominio).toList();
    }

    @Override
    public List<Pedido> buscarSinPagarCreadosAntesDe(OffsetDateTime limite) {
        return pedidoJpaRepository.findByEstadoAndCreadoEnBefore(EstadoPedido.CREADO.name(), limite).stream()
                .map(this::aDominio).toList();
    }

    private PedidoEntity aEntity(Pedido dominio) {
        PedidoEntity entity = (dominio.getId() != null)
                ? pedidoJpaRepository.findById(dominio.getId()).orElseGet(PedidoEntity::new)
                : new PedidoEntity();

        entity.setId(dominio.getId());
        entity.setUsuarioId(dominio.getUsuarioId());
        entity.setEstado(dominio.getEstado().name());
        entity.setTotal(dominio.getTotal().monto());
        entity.setCreadoEn(dominio.getCreadoEn());
        entity.setPagoTransaccionId(dominio.getTransaccionId());
        entity.setPagoMetodo(dominio.getMetodoPago());
        entity.setPagadoEn(dominio.getPagadoEn());

        entity.getItems().clear();
        for (PedidoItem item : dominio.getItems()) {
            entity.agregarItem(new PedidoItemEntity(
                    entity, item.productoId(), item.cantidad(), item.precioUnitario().monto()));
        }

        return entity;
    }

    private Pedido aDominio(PedidoEntity entity) {
        List<PedidoItem> items = entity.getItems().stream()
                .map(i -> new PedidoItem(i.getProductoId(), i.getCantidad(), new Dinero(i.getPrecioUnit())))
                .toList();

        return new Pedido(
                entity.getId(),
                entity.getUsuarioId(),
                EstadoPedido.valueOf(entity.getEstado()),
                items,
                entity.getCreadoEn())
                .conPago(entity.getPagoTransaccionId(), entity.getPagoMetodo(), entity.getPagadoEn());
    }
}
