package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModerarProductoServiceTest {

    private final ProductoRepositoryPort productos = mock(ProductoRepositoryPort.class);
    private final ModerarProductoService service = new ModerarProductoService(productos);

    private static Producto producto(long id) {
        return new Producto(id, 5L, "Sérum", "Marca", "Serums", Dinero.de(10000), 10, null, null, true,
                List.of(new ComposicionProducto(new Ingrediente(1L, new Inci("aqua"), "Agua"), null)), null);
    }

    @Test
    void bloqueado_saleDelCatalogoYElVendedorNoPuedePublicarlo() {
        Producto p = producto(1L);
        when(productos.buscarPorId(1L)).thenReturn(Optional.of(p));
        when(productos.guardar(any())).thenAnswer(i -> i.getArgument(0));

        service.banear(1L, "Sin registro sanitario");

        assertFalse(p.estaEnCatalogo());
        assertTrue(p.isActivo(), "no pisa lo que decidió el vendedor");
        assertThrows(ReglaNegocioException.class, p::publicar);
    }

    @Test
    void alDesbloquear_vuelveComoLoTeniaElVendedor() {
        Producto deBaja = producto(1L);
        deBaja.despublicar();
        deBaja.banear("x");
        when(productos.buscarPorId(1L)).thenReturn(Optional.of(deBaja));
        when(productos.guardar(any())).thenAnswer(i -> i.getArgument(0));

        service.desbanear(1L);

        assertFalse(deBaja.isBaneado());
        assertFalse(deBaja.isActivo(), "seguía dado de baja por el vendedor");
    }

    @Test
    void bloquearSinMotivo_falla() {
        when(productos.buscarPorId(1L)).thenReturn(Optional.of(producto(1L)));
        assertThrows(ReglaNegocioException.class, () -> service.banear(1L, " "));
    }

    @Test
    void reactivarVendedor_noDevuelveLoBloqueadoPorOtroMotivo() {
        Producto porSuspension = producto(1L);
        Producto porFraude = producto(2L);
        porFraude.banear("Fraude");
        when(productos.buscarPorVendedor(5L)).thenReturn(List.of(porSuspension, porFraude));

        service.suspenderVendedor(5L);
        assertTrue(porSuspension.isBaneado());
        assertEquals("Fraude", porFraude.getMotivoBaneo(), "suspender no pisa un bloqueo anterior");

        service.reactivarVendedor(5L);
        assertFalse(porSuspension.isBaneado());
        assertTrue(porFraude.isBaneado());
    }
}
