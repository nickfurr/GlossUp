package com.glossup.compatibilidad.application.service;

import com.glossup.compatibilidad.domain.model.Alerta;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Motor de reglas de compatibilidad producto–perfil (HU6).
 *
 * <p>Es determinista y explicable (no probabilístico): toda alerta nace de un hecho concreto del
 * perfil, por eso cumple RNF-01 (0 falsos negativos críticos) y RNF-05 (causa explicada). Las
 * alergias siempre son ALTA (bloqueo); los irritantes toman la severidad de su regla.</p>
 */
@Service
public class EvaluarCompatibilidadService implements EvaluarCompatibilidadUseCase {

    private final PerfilConsultaPort perfil;
    private final CatalogoConsultaPort catalogo;
    private final ReglaRepositoryPort reglas;

    public EvaluarCompatibilidadService(PerfilConsultaPort perfil, CatalogoConsultaPort catalogo,
                                        ReglaRepositoryPort reglas) {
        this.perfil = perfil;
        this.catalogo = catalogo;
        this.reglas = reglas;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoCompatibilidad evaluarProductoPerfil(Long usuarioId, Long productoId) {
        CatalogoConsultaPort.ProductoConsulta producto = catalogo.obtener(productoId); // 404 si no existe
        PerfilConsultaPort.PerfilConsulta datos = perfil.obtener(usuarioId);
        Set<String> ingredientes = Set.copyOf(producto.ingredientesInci());

        ResultadoCompatibilidad resultado = ResultadoCompatibilidad.vacio();

        // 1) Alergias declaradas → siempre crítico (RNF-01: ningún falso negativo aquí). Cada
        //    alérgeno es un grupo (ej. parabenos) que se expande a todos sus nombres INCI.
        Map<String, Set<String>> porAlergeno = reglas.ingredientesPorAlergeno();
        for (String alergeno : datos.alergias()) {
            for (String inci : porAlergeno.getOrDefault(alergeno, Set.of())) {
                if (ingredientes.contains(inci)) {
                    String grupo = datos.etiqueta(alergeno);
                    resultado.agregar(new Alerta(Severidad.ALTA,
                            "«" + producto.nombre() + "» contiene " + inci + " (" + grupo + "), al que registraste alergia.",
                            "Alergia declarada en tu perfil: " + grupo + ". " + inci + " pertenece a ese grupo."));
                }
            }
        }

        // 2) Irritantes conocidos para una condición del perfil (tipo de piel o afección).
        for (ReglaPerfil regla : reglas.reglasPerfil()) {
            if (ingredientes.contains(regla.getIngrediente()) && datos.condiciones().contains(regla.getCondicion())) {
                resultado.agregar(new Alerta(regla.getSeveridad(),
                        "«" + producto.nombre() + "» contiene " + regla.getIngrediente()
                                + ", no recomendado para tu condición: " + datos.etiqueta(regla.getCondicion()) + ".",
                        regla.getMotivo()));
            }
        }

        return resultado;
    }
}
