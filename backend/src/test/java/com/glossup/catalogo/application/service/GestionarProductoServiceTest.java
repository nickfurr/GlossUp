package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.ImagenProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ActualizarProductoCommand;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.out.IngredienteRepositoryPort;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarProductoServiceTest {

    @Mock
    private ProductoRepositoryPort productoRepository;

    @Mock
    private IngredienteRepositoryPort ingredienteRepository;

    private GestionarProductoService service;

    @BeforeEach
    void setUp() {
        service = new GestionarProductoService(productoRepository, ingredienteRepository);
    }

    private Producto crearProductoExistente(Long id, Long vendedorId) {
        return new Producto(
                id, vendedorId, "Serum Original", "GlossLab", "Serum",
                Dinero.de(50000), 10, 5.0, "INVIMA-111", true,
                List.of(new ImagenProducto("https://cdn.glossup.co/old.webp")),
                List.of(new ComposicionProducto(new Ingrediente(1L, new Inci("Aqua"), "Agua"), null)),
                null
        );
    }

    @Test
    void actualizar_cuandoVendedorEsPropietarioYDatosValidos_actualizaYPersiste() {
        Producto existente = crearProductoExistente(100L, 5L);
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));
        when(ingredienteRepository.buscarPorInci(new Inci("niacinamide")))
                .thenReturn(Optional.of(new Ingrediente(2L, new Inci("niacinamide"), "Niacinamida · vitamina B3")));
        when(productoRepository.guardar(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActualizarProductoCommand command = new ActualizarProductoCommand(
                100L, 5L, "Serum Renovado", "GlossLab Pro", "Cuidado Facial",
                BigDecimal.valueOf(75000), 30, 4.5, "INVIMA-222",
                List.of("https://cdn.glossup.co/new.webp"),
                List.of(new RegistrarProductoCommand.IngredienteComando("Niacinamide", 10.0))
        );

        Producto resultado = service.actualizar(command);

        assertEquals("Serum Renovado", resultado.getNombre());
        assertEquals(30, resultado.getStock());
        assertEquals(1, resultado.getImagenes().size());
        assertEquals("https://cdn.glossup.co/new.webp", resultado.getImagenes().get(0).url());
        assertEquals(1, resultado.getComposicion().size());
        assertTrue(resultado.contieneIngrediente(new Inci("niacinamide")));
        verify(productoRepository).guardar(existente);
    }

    @Test
    void actualizar_cuandoProductoNoExiste_lanzaNotFoundException() {
        when(productoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

        ActualizarProductoCommand command = new ActualizarProductoCommand(
                999L, 5L, "Serum", "GlossLab", "Serum",
                BigDecimal.valueOf(50000), 10, 5.0, "INV",
                List.of(),
                List.of(new RegistrarProductoCommand.IngredienteComando("Aqua", null))
        );

        assertThrows(NotFoundException.class, () -> service.actualizar(command));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void actualizar_cuandoVendedorNoEsPropietario_lanzaAccessDeniedException() {
        Producto existente = crearProductoExistente(100L, 5L);
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));

        ActualizarProductoCommand command = new ActualizarProductoCommand(
                100L, 99L, "Intento Hack", "OtraMarca", "Serum",
                BigDecimal.valueOf(1000), 1, 5.0, "INV",
                List.of(),
                List.of(new RegistrarProductoCommand.IngredienteComando("Aqua", null))
        );

        assertThrows(AccessDeniedException.class, () -> service.actualizar(command));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void actualizar_cuandoFichaInciEstaVacia_lanzaReglaNegocioException() {
        Producto existente = crearProductoExistente(100L, 5L);
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));

        ActualizarProductoCommand command = new ActualizarProductoCommand(
                100L, 5L, "Serum Sin INCI", "GlossLab", "Serum",
                BigDecimal.valueOf(50000), 10, 5.0, "INV",
                List.of(), List.of()
        );

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(command));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void eliminar_cuandoVendedorEsPropietario_despublicaYGuardaConActivoFalse() {
        Producto existente = crearProductoExistente(100L, 5L);
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));

        service.eliminar(100L, 5L);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).guardar(captor.capture());
        assertFalse(captor.getValue().isActivo(), "El producto debe quedar despublicado (activo = false)");
    }

    @Test
    void eliminar_cuandoVendedorNoEsPropietario_lanzaAccessDeniedException() {
        Producto existente = crearProductoExistente(100L, 5L);
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));

        assertThrows(AccessDeniedException.class, () -> service.eliminar(100L, 99L));
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    void publicar_propietario_vuelveAlCatalogo() {
        Producto existente = crearProductoExistente(100L, 5L);
        existente.despublicar();
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));
        when(productoRepository.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        org.junit.jupiter.api.Assertions.assertTrue(service.publicar(100L, 5L).isActivo());
    }

    @Test
    void publicar_deOtroVendedor_seRechazaSinGuardar() {
        Producto existente = crearProductoExistente(100L, 5L);
        existente.despublicar();
        when(productoRepository.buscarPorId(100L)).thenReturn(Optional.of(existente));

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.publicar(100L, 6L));
        org.junit.jupiter.api.Assertions.assertFalse(existente.isActivo());
        org.mockito.Mockito.verify(productoRepository, org.mockito.Mockito.never()).guardar(any());
    }
}
