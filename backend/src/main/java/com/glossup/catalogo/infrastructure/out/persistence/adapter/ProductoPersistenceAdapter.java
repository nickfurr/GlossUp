package com.glossup.catalogo.infrastructure.out.persistence.adapter;

import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.ImagenProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.out.persistence.entity.ImagenProductoEntity;
import com.glossup.catalogo.infrastructure.out.persistence.entity.IngredienteEntity;
import com.glossup.catalogo.infrastructure.out.persistence.entity.ProductoEntity;
import com.glossup.catalogo.infrastructure.out.persistence.entity.ProductoIngredienteEntity;
import com.glossup.catalogo.infrastructure.out.persistence.repository.IngredienteJpaRepository;
import com.glossup.catalogo.infrastructure.out.persistence.repository.ProductoJpaRepository;
import com.glossup.shared.domain.Dinero;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class ProductoPersistenceAdapter implements ProductoRepositoryPort {

    private final ProductoJpaRepository productoJpaRepository;
    private final IngredienteJpaRepository ingredienteJpaRepository;

    public ProductoPersistenceAdapter(ProductoJpaRepository productoJpaRepository,
                                      IngredienteJpaRepository ingredienteJpaRepository) {
        this.productoJpaRepository = productoJpaRepository;
        this.ingredienteJpaRepository = ingredienteJpaRepository;
    }

    @Override
    public Producto guardar(Producto producto) {
        ProductoEntity entity = aEntity(producto);
        ProductoEntity guardado = productoJpaRepository.save(entity);
        return aDominio(guardado);
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoJpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public List<Producto> buscarPorVendedor(Long vendedorId) {
        return productoJpaRepository.findByVendedorId(vendedorId).stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public List<Producto> buscarPorPalabraClave(String palabraClave) {
        String sinTildes = Normalizer.normalize(palabraClave.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return productoJpaRepository.buscarPorPalabraClave(sinTildes).stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public List<Producto> buscarPorIds(Collection<Long> ids) {
        return productoJpaRepository.findAllById(ids).stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public List<Producto> listarTodos() {
        return productoJpaRepository.findAll().stream()
                .map(this::aDominio)
                .toList();
    }

    private ProductoEntity aEntity(Producto dominio) {
        ProductoEntity entity = (dominio.getId() != null)
                ? productoJpaRepository.findById(dominio.getId()).orElseGet(ProductoEntity::new)
                : new ProductoEntity();

        entity.setId(dominio.getId());
        entity.setVendedorId(dominio.getVendedorId());
        entity.setNombre(dominio.getNombre());
        entity.setMarca(dominio.getMarca());
        entity.setCategoria(dominio.getCategoria());
        entity.setPrecio(dominio.getPrecio().monto());
        entity.setStock(dominio.getStock());
        entity.setPh(dominio.getPh());
        entity.setRegistroInvima(dominio.getRegistroInvima());
        entity.setActivo(dominio.isActivo());
        entity.setCreadoEn(dominio.getCreadoEn());

        // Sincronizar lista de imágenes
        entity.getImagenes().clear();
        int orden = 0;
        for (ImagenProducto img : dominio.getImagenes()) {
            entity.agregarImagen(new ImagenProductoEntity(entity, img.url(), orden++));
        }

        // Sincronizar lista de composición INCI
        entity.getComposicion().clear();
        for (ComposicionProducto comp : dominio.getComposicion()) {
            Ingrediente ing = comp.ingrediente();
            IngredienteEntity ingEntity = null;
            if (ing.getId() != null) {
                ingEntity = ingredienteJpaRepository.findById(ing.getId()).orElse(null);
            }
            if (ingEntity == null) {
                ingEntity = ingredienteJpaRepository.findByNombreInciIgnoreCase(ing.getInci().nombre())
                        .orElseGet(() -> ingredienteJpaRepository.save(
                                new IngredienteEntity(null, ing.getInci().nombre(), ing.getDescripcion())
                        ));
            }
            entity.agregarIngrediente(new ProductoIngredienteEntity(entity, ingEntity, comp.concentracion()));
        }

        return entity;
    }

    private Producto aDominio(ProductoEntity entity) {
        List<ImagenProducto> imagenes = entity.getImagenes().stream()
                .map(img -> new ImagenProducto(img.getUrl()))
                .toList();

        List<ComposicionProducto> composicion = entity.getComposicion().stream()
                .map(pi -> {
                    Ingrediente ing = new Ingrediente(
                            pi.getIngrediente().getId(),
                            new Inci(pi.getIngrediente().getNombreInci()),
                            pi.getIngrediente().getDescripcion()
                    );
                    return new ComposicionProducto(ing, pi.getConcentracion());
                })
                .toList();

        return new Producto(
                entity.getId(),
                entity.getVendedorId(),
                entity.getNombre(),
                entity.getMarca(),
                entity.getCategoria(),
                new Dinero(entity.getPrecio()),
                entity.getStock(),
                entity.getPh(),
                entity.getRegistroInvima(),
                entity.isActivo(),
                imagenes,
                composicion,
                entity.getCreadoEn()
        );
    }
}
