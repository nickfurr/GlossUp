package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MotorRecomendacion.Candidato;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.CatalogoRecomendablePort;
import com.glossup.recomendacion.domain.port.out.EvaluacionPerfilPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecomendarProductosServiceTest {

    private final CatalogoRecomendablePort catalogo = mock(CatalogoRecomendablePort.class);
    private final EvaluacionPerfilPort perfil = mock(EvaluacionPerfilPort.class);
    private final PedidoRepositoryPort pedidos = mock(PedidoRepositoryPort.class);
    private final RecomendarProductosService service = new RecomendarProductosService(catalogo, perfil, pedidos);

    private static Pedido pedido(long usuario, EstadoPedido estado, long producto) {
        return new Pedido(null, usuario, estado, List.of(new PedidoItem(producto, 1, Dinero.de(1000.0))), OffsetDateTime.now());
    }

    @Test
    void soloCuentanLosPedidosPagados() {
        OffsetDateTime t = OffsetDateTime.now();
        when(catalogo.disponibles()).thenReturn(List.of(new Candidato(10L, "A", "X", Set.of("a"), t),
                new Candidato(20L, "B", "Y", Set.of("b"), t)));
        when(perfil.evaluar(1L, List.of(10L, 20L))).thenReturn(new EvaluacionPerfilPort.Evaluacion(true, Map.of()));
        // El cliente 1 "compró" el 10 en un pedido sin pagar: no debe excluirlo ni contar como compra.
        when(pedidos.listarTodos()).thenReturn(List.of(pedido(1, EstadoPedido.CREADO, 10), pedido(2, EstadoPedido.PAGADO, 20)));

        var r = service.recomendar(1L, 6);
        assertTrue(r.perfilVacio());
        assertEquals(List.of(20L, 10L), r.recomendaciones().stream().map(x -> x.productoId()).toList());
        assertEquals("De los más comprados en GlossUP", r.recomendaciones().get(0).razon());
    }

    @Test
    void elLimiteSeValida() {
        assertThrows(ReglaNegocioException.class, () -> service.recomendar(1L, 0));
        assertThrows(ReglaNegocioException.class, () -> service.recomendar(1L, 25));
        verifyNoInteractions(catalogo);
    }
}
