package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.EnvioEmbeddable;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.PedidoItemEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.repository.PedidoJpaRepository;
import com.glossup.shared.domain.Dinero;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Override
    public List<Pedido> buscarEnviosSinNotificar(OffsetDateTime limite, int maxIntentos) {
        return pedidoJpaRepository.findByEstadoAndEnvioEstadoAndEnvioIntentosLessThanAndPagadoEnBefore(
                        EstadoPedido.PAGADO.name(), EstadoEnvio.PREPARANDO.name(), maxIntentos, limite).stream()
                .map(this::aDominio).toList();
    }

    @Override
    public List<Pedido> buscarEnviosEnCurso() {
        return pedidoJpaRepository.findByEnvioEstadoIn(
                        List.of(EstadoEnvio.LISTO_PARA_ENVIO.name(), EstadoEnvio.EN_CAMINO.name())).stream()
                .map(this::aDominio).toList();
    }

    @Override
    public Optional<Pedido> buscarPorGuia(String guia) {
        return pedidoJpaRepository.findByEnvioGuia(guia).map(this::aDominio);
    }

    @Override
    public List<Pedido> buscarPagadosConProductos(Set<Long> productoIds) {
        return productoIds.isEmpty() ? List.of()
                : pedidoJpaRepository.buscarPagadosConProductos(productoIds).stream().map(this::aDominio).toList();
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
        entity.setEnvio(aEmbeddable(dominio.getEnvio()));

        entity.getItems().clear();
        for (PedidoItem item : dominio.getItems()) {
            entity.agregarItem(new PedidoItemEntity(
                    entity, item.productoId(), item.nombre(), item.cantidad(), item.precioUnitario().monto()));
        }

        return entity;
    }

    private Pedido aDominio(PedidoEntity entity) {
        List<PedidoItem> items = entity.getItems().stream()
                .map(i -> new PedidoItem(i.getProductoId(), i.getNombre(), i.getCantidad(), new Dinero(i.getPrecioUnit())))
                .toList();

        return new Pedido(
                entity.getId(),
                entity.getUsuarioId(),
                EstadoPedido.valueOf(entity.getEstado()),
                items,
                entity.getCreadoEn(),
                aEnvio(entity.getEnvio()))
                .conPago(entity.getPagoTransaccionId(), entity.getPagoMetodo(), entity.getPagadoEn());
    }

    private static EnvioEmbeddable aEmbeddable(Envio e) {
        if (e == null) {
            return null;
        }
        EnvioEmbeddable x = new EnvioEmbeddable();
        Direccion d = e.getDestino();
        x.nombreRecibe = d.nombreRecibe();
        x.telefono = d.telefono();
        x.departamento = d.departamento();
        x.ciudad = d.ciudad();
        x.barrio = d.barrio();
        x.direccion = d.direccion();
        x.complemento = d.complemento();
        x.indicaciones = d.indicaciones();
        x.metodo = e.getMetodo().name();
        x.costo = e.getCosto().monto();
        x.fechaEstimada = e.getFechaEstimada();
        x.estado = e.getEstado().name();
        x.transportadora = e.getTransportadora();
        x.guia = e.getGuia();
        x.intentos = e.getIntentos();
        x.notificadoEn = e.getNotificadoEn();
        x.enviadoEn = e.getEnviadoEn();
        x.entregadoEn = e.getEntregadoEn();
        x.noEntregadoEn = e.getNoEntregadoEn();
        return x;
    }

    private static Envio aEnvio(EnvioEmbeddable x) {
        if (x == null) {
            return null;
        }
        Direccion destino = new Direccion(null, null, x.nombreRecibe, x.telefono, x.departamento, x.ciudad,
                x.barrio, x.direccion, x.complemento, x.indicaciones, false);
        return new Envio(destino, MetodoEnvio.valueOf(x.metodo), new Dinero(x.costo), x.fechaEstimada)
                .conSeguimiento(EstadoEnvio.valueOf(x.estado), x.transportadora, x.guia, x.intentos,
                        x.notificadoEn, x.enviadoEn, x.entregadoEn, x.noEntregadoEn);
    }
}
