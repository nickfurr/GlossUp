package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.MotorRecomendacion;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.in.RecomendarProductosUseCase;
import com.glossup.recomendacion.domain.port.out.CatalogoRecomendablePort;
import com.glossup.recomendacion.domain.port.out.EvaluacionPerfilPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Arma los datos del recomendador (catálogo, evaluación del perfil y pedidos pagados) y delega en el motor. */
@Service
public class RecomendarProductosService implements RecomendarProductosUseCase {

    static final int MAX_LIMITE = 24;
    private static final Set<EstadoPedido> PAGADOS = Set.of(EstadoPedido.PAGADO, EstadoPedido.ENVIADO);

    private final CatalogoRecomendablePort catalogo;
    private final EvaluacionPerfilPort perfil;
    private final PedidoRepositoryPort pedidos;

    public RecomendarProductosService(CatalogoRecomendablePort catalogo, EvaluacionPerfilPort perfil,
                                      PedidoRepositoryPort pedidos) {
        this.catalogo = catalogo;
        this.perfil = perfil;
        this.pedidos = pedidos;
    }

    @Override
    @Transactional // no readOnly: leer el perfil de un cliente nuevo lo crea vacío
    public ResultadoRecomendacion recomendar(Long usuarioId, int limite) {
        if (limite < 1 || limite > MAX_LIMITE) {
            throw new ReglaNegocioException("Pide entre 1 y " + MAX_LIMITE + " recomendaciones");
        }
        // shortcut: recorre el catálogo y los pedidos pagados en cada consulta (cientos de filas hoy); con miles,
        // precalcular la popularidad y las co-compras en una tarea programada.
        List<MotorRecomendacion.Candidato> candidatos = catalogo.disponibles();
        EvaluacionPerfilPort.Evaluacion evaluacion = perfil.evaluar(usuarioId,
                candidatos.stream().map(MotorRecomendacion.Candidato::id).toList());
        List<MotorRecomendacion.Compra> compras = pedidos.listarTodos().stream()
                .filter(p -> PAGADOS.contains(p.getEstado()))
                .map(p -> new MotorRecomendacion.Compra(p.getUsuarioId(),
                        p.getItems().stream().map(PedidoItem::productoId).collect(Collectors.toSet())))
                .toList();
        return new ResultadoRecomendacion(evaluacion.perfilVacio(), MotorRecomendacion.recomendar(
                usuarioId, candidatos, evaluacion.perfilVacio(), evaluacion.calificaciones(), compras, limite));
    }
}
