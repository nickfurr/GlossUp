package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.model.Carrito;
import com.glossup.recomendacion.domain.model.ItemCarrito;
import com.glossup.recomendacion.domain.port.out.CarritoRepositoryPort;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.CarritoEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.entity.CarritoItemEntity;
import com.glossup.recomendacion.infrastructure.out.persistence.repository.CarritoJpaRepository;
import com.glossup.shared.domain.Dinero;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CarritoPersistenceAdapter implements CarritoRepositoryPort {

    private final CarritoJpaRepository carritoJpaRepository;

    public CarritoPersistenceAdapter(CarritoJpaRepository carritoJpaRepository) {
        this.carritoJpaRepository = carritoJpaRepository;
    }

    @Override
    public Optional<Carrito> buscarPorUsuario(Long usuarioId) {
        return carritoJpaRepository.findByUsuarioId(usuarioId).map(this::aDominio);
    }

    @Override
    public Carrito guardar(Carrito carrito) {
        CarritoEntity entity = carritoJpaRepository.findByUsuarioId(carrito.getUsuarioId())
                .orElseGet(CarritoEntity::new);

        entity.setUsuarioId(carrito.getUsuarioId());
        entity.setActualizadoEn(OffsetDateTime.now());

        entity.getItems().clear();
        for (ItemCarrito item : carrito.getItems()) {
            entity.agregarItem(new CarritoItemEntity(
                    entity, item.getProductoId(), item.getCantidad(), item.getPrecioUnitario().monto()));
        }

        return aDominio(carritoJpaRepository.save(entity));
    }

    private Carrito aDominio(CarritoEntity entity) {
        List<ItemCarrito> items = entity.getItems().stream()
                .map(i -> new ItemCarrito(i.getProductoId(), new Dinero(i.getPrecioUnit()), i.getCantidad()))
                .toList();
        return new Carrito(entity.getUsuarioId(), items);
    }
}
