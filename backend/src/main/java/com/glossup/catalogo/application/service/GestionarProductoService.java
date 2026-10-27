package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.ImagenProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ActualizarProductoCommand;
import com.glossup.catalogo.domain.port.in.GestionarProductoUseCase;
import com.glossup.catalogo.domain.port.out.IngredienteRepositoryPort;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de aplicación que implementa {@link GestionarProductoUseCase} (HU4 - SCRUM-31).
 * Orquesta la modificación integral y eliminación lógica (soft-delete) de productos del catálogo,
 * verificando propiedad del vendedor (RNF-14) e integridad de la ficha INCI (RNF-04).
 */
@Service
@Transactional
public class GestionarProductoService implements GestionarProductoUseCase {

    private final ProductoRepositoryPort productoRepository;
    private final IngredienteRepositoryPort ingredienteRepository;

    public GestionarProductoService(ProductoRepositoryPort productoRepository,
                                    IngredienteRepositoryPort ingredienteRepository) {
        this.productoRepository = productoRepository;
        this.ingredienteRepository = ingredienteRepository;
    }

    @Override
    public Producto actualizar(ActualizarProductoCommand command) {
        Producto producto = productoRepository.buscarPorId(command.productoId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + command.productoId()));

        if (!producto.perteneceAVendedor(command.vendedorId())) {
            throw new AccessDeniedException("Acceso denegado: no puedes modificar un producto de otro vendedor");
        }

        List<ComposicionProducto> nuevaComposicion = FichaInci.resolver(command.ingredientes(), ingredienteRepository);

        List<ImagenProducto> nuevasImagenes = new ArrayList<>();
        if (command.urlsImagenes() != null) {
            for (String url : command.urlsImagenes()) {
                nuevasImagenes.add(new ImagenProducto(url));
            }
        }

        producto.actualizar(
                command.nombre(),
                command.marca(),
                command.categoria(),
                new Dinero(command.precio()),
                command.stock(),
                command.ph(),
                command.registroInvima(),
                nuevasImagenes,
                nuevaComposicion
        );
        producto.describir(command.descripcion());

        return productoRepository.guardar(producto);
    }

    @Override
    public void eliminar(Long productoId, Long vendedorId) {
        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));

        if (!producto.perteneceAVendedor(vendedorId)) {
            throw new AccessDeniedException("Acceso denegado: no puedes eliminar un producto de otro vendedor");
        }

        producto.despublicar();
        productoRepository.guardar(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ingrediente> buscarIngredientes(String texto) {
        return texto == null || texto.isBlank() ? List.of() : ingredienteRepository.buscar(texto);
    }
}
