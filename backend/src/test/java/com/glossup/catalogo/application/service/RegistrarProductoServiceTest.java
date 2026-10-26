package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.out.IngredienteRepositoryPort;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarProductoServiceTest {

    @Mock
    private ProductoRepositoryPort productoRepository;

    @Mock
    private IngredienteRepositoryPort ingredienteRepository;

    @InjectMocks
    private RegistrarProductoService service;

    @Test
    void debeRegistrarProductoExitosamenteConImagenesEFichaInci() {
        // Arrange
        Ingrediente ingAscorbic = new Ingrediente(10L, new Inci("ascorbic acid"), "Vitamina C");
        Ingrediente ingAqua = new Ingrediente(11L, new Inci("aqua"), "Agua purificada");

        when(ingredienteRepository.buscarPorInci(new Inci("ascorbic acid"))).thenReturn(Optional.of(ingAscorbic));
        when(ingredienteRepository.buscarPorInci(new Inci("aqua"))).thenReturn(Optional.of(ingAqua));

        when(productoRepository.guardar(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RegistrarProductoCommand command = new RegistrarProductoCommand(
                7L,
                "Serum Vitamina C",
                "GlossLab",
                "serum",
                BigDecimal.valueOf(89000.0),
                50,
                3.5,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img1.webp", "https://cdn.glossup.co/img2.webp"),
                List.of(
                        new RegistrarProductoCommand.IngredienteComando("Ascorbic Acid", 15.0),
                        new RegistrarProductoCommand.IngredienteComando("Aqua", null)
                )
        );

        // Act
        Producto resultado = service.registrar(command);

        // Assert
        assertNotNull(resultado);
        assertEquals(7L, resultado.getVendedorId());
        assertEquals("Serum Vitamina C", resultado.getNombre());
        assertTrue(resultado.isActivo());
        assertEquals(2, resultado.getImagenes().size());
        assertEquals(2, resultado.getComposicion().size());

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository, times(1)).guardar(captor.capture());
        Producto productoCapturado = captor.getValue();
        assertEquals("Serum Vitamina C", productoCapturado.getNombre());
        assertEquals(2, productoCapturado.getImagenes().size());
    }

    @Test
    void debeLanzarExcepcionSiComandoNoTieneIngredientes() {
        RegistrarProductoCommand command = new RegistrarProductoCommand(
                1L, "Gel", "Marca", "cat", BigDecimal.valueOf(20000), 10, 5.0, null,
                List.of("https://cdn.glossup.co/img.png"),
                List.of()
        );

        assertThrows(ReglaNegocioException.class, () -> service.registrar(command));
        verifyNoInteractions(ingredienteRepository);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void debeLanzarExcepcionSiListaDeIngredientesEsNula() {
        RegistrarProductoCommand command = new RegistrarProductoCommand(
                1L, "Gel", "Marca", "cat", BigDecimal.valueOf(20000), 10, 5.0, null,
                List.of(), null
        );

        assertThrows(ReglaNegocioException.class, () -> service.registrar(command));
        verifyNoInteractions(ingredienteRepository);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void debePermitirRegistrarProductoSinImagenes() {
        Ingrediente ing = new Ingrediente(1L, new Inci("niacinamide"), "Vitamina B3");
        when(ingredienteRepository.buscarPorInci(new Inci("niacinamide"))).thenReturn(Optional.of(ing));
        when(productoRepository.guardar(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarProductoCommand command = new RegistrarProductoCommand(
                2L, "Serum Niacinamida", "GlossLab", "serum", BigDecimal.valueOf(45000), 10, 5.5, null,
                null,
                List.of(new RegistrarProductoCommand.IngredienteComando("Niacinamide", 10.0))
        );

        Producto resultado = service.registrar(command);

        assertNotNull(resultado);
        assertTrue(resultado.getImagenes().isEmpty());
        assertEquals(1, resultado.getComposicion().size());
        verify(productoRepository, times(1)).guardar(any(Producto.class));
    }

    @Test
    void ingredientesFueraDelCatalogo_seRechazanTodosJuntosSinGuardar() {
        when(ingredienteRepository.buscarPorInci(new Inci("aqua")))
                .thenReturn(Optional.of(new Ingrediente(11L, new Inci("aqua"), "Agua")));
        when(ingredienteRepository.buscarPorInci(new Inci("glicerina"))).thenReturn(Optional.empty());
        when(ingredienteRepository.buscarPorInci(new Inci("polvo magico"))).thenReturn(Optional.empty());

        RegistrarProductoCommand command = new RegistrarProductoCommand(
                2L, "Crema", "GlossLab", "crema", BigDecimal.valueOf(30000), 5, null, null, null,
                List.of(new RegistrarProductoCommand.IngredienteComando("Aqua", null),
                        new RegistrarProductoCommand.IngredienteComando("Glicerina", null),
                        new RegistrarProductoCommand.IngredienteComando("Polvo magico", null)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.registrar(command));
        assertTrue(ex.getMessage().contains("Glicerina"));
        assertTrue(ex.getMessage().contains("Polvo magico"));
        verifyNoInteractions(productoRepository);
    }
}
