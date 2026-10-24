package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Carrito;
import com.glossup.recomendacion.domain.port.out.CarritoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarCarritoServiceTest {

    @Mock
    private CarritoRepositoryPort carritoRepository;

    @Mock
    private InventarioPort inventario;

    private GestionarCarritoService service;

    @BeforeEach
    void setUp() {
        service = new GestionarCarritoService(carritoRepository, inventario);
        lenient().when(carritoRepository.guardar(any(Carrito.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void verCarrito_sinCarritoPrevio_devuelveVacioSinPersistir() {
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.empty());

        Carrito carrito = service.verCarrito(7L);

        assertTrue(carrito.estaVacio());
        verify(carritoRepository, never()).guardar(any());
    }

    @Test
    void agregarItem_nuevoProducto_consultaPrecioYPersiste() {
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.empty());
        when(inventario.consultarPrecio(100L)).thenReturn(Dinero.de(20000));

        Carrito carrito = service.agregarItem(7L, 100L, 2);

        assertEquals(1, carrito.getItems().size());
        assertEquals(Dinero.de(40000), carrito.total());
        verify(inventario, never()).reservar(anyLong(), anyInt()); // agregar NO reserva stock
        verify(carritoRepository).guardar(any(Carrito.class));
    }

    @Test
    void agregarItem_productoExistente_acumulaCantidad() {
        Carrito existente = Carrito.vacio(7L);
        existente.agregar(100L, Dinero.de(20000), 1);
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(existente));
        when(inventario.consultarPrecio(100L)).thenReturn(Dinero.de(20000));

        Carrito carrito = service.agregarItem(7L, 100L, 3);

        assertEquals(1, carrito.getItems().size());
        assertEquals(4, carrito.getItems().get(0).getCantidad());
    }

    @Test
    void cambiarCantidad_productoNoEnCarrito_lanzaReglaNegocio() {
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(Carrito.vacio(7L)));

        assertThrows(ReglaNegocioException.class, () -> service.cambiarCantidad(7L, 999L, 2));
        verify(carritoRepository, never()).guardar(any());
    }

    @Test
    void quitarItem_dejaElCarritoSinEseProducto() {
        Carrito existente = Carrito.vacio(7L);
        existente.agregar(100L, Dinero.de(20000), 1);
        existente.agregar(200L, Dinero.de(5000), 1);
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(existente));

        Carrito carrito = service.quitarItem(7L, 100L);

        assertEquals(1, carrito.getItems().size());
        assertEquals(200L, carrito.getItems().get(0).getProductoId());
    }

    @Test
    void vaciar_dejaElCarritoVacio() {
        Carrito existente = Carrito.vacio(7L);
        existente.agregar(100L, Dinero.de(20000), 1);
        when(carritoRepository.buscarPorUsuario(7L)).thenReturn(Optional.of(existente));

        Carrito carrito = service.vaciar(7L);

        assertTrue(carrito.estaVacio());
        verify(carritoRepository).guardar(any(Carrito.class));
    }
}
