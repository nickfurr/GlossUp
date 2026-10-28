package com.glossup.compatibilidad.application.service;

import com.glossup.compatibilidad.domain.model.Alerta;
import com.glossup.compatibilidad.domain.model.Beneficio;
import com.glossup.compatibilidad.domain.model.Cruce;
import com.glossup.compatibilidad.domain.model.ReglaIncompatibilidad;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ReglaRango;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Motor de reglas de compatibilidad producto–perfil (HU6 alergias e irritantes, HU7 tipo de piel,
 * comedogenia y pH).
 *
 * <p>Es determinista y explicable (no probabilístico): toda alerta nace de un hecho concreto del
 * perfil, por eso cumple RNF-01 (0 falsos negativos críticos) y RNF-05 (causa explicada). Las
 * alergias siempre son ALTA (bloqueo); el resto toma la severidad de su regla.</p>
 */
@Service
public class EvaluarCompatibilidadService implements EvaluarCompatibilidadUseCase {

    /** Límite superior del pH de piel sana (glosario y {@code Ph.esFisiologico}). */
    private static final double PH_PIEL_MAXIMO = 5.75;
    /** Desde aquí un producto es alcalino para la piel (jabones: 9–10). */
    private static final double PH_ALCALINO = 7.5;

    private final PerfilConsultaPort perfil;
    private final CatalogoConsultaPort catalogo;
    private final ReglaRepositoryPort reglas;

    public EvaluarCompatibilidadService(PerfilConsultaPort perfil, CatalogoConsultaPort catalogo,
                                        ReglaRepositoryPort reglas) {
        this.perfil = perfil;
        this.catalogo = catalogo;
        this.reglas = reglas;
    }

    /** Base de conocimiento completa; se carga una vez por petición. */
    private record Conocimiento(List<ReglaPerfil> reglasPerfil, Map<String, Set<String>> porAlergeno,
                                List<Beneficio> beneficios, List<ReglaRango> rangos,
                                Map<String, Integer> comedogenicidad) {}

    private Conocimiento cargarConocimiento() {
        return new Conocimiento(reglas.reglasPerfil(), reglas.ingredientesPorAlergeno(), reglas.beneficios(),
                reglas.reglasRango(), reglas.comedogenicidad());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoCompatibilidad evaluarProductoPerfil(Long usuarioId, Long productoId) {
        CatalogoConsultaPort.ProductoConsulta producto = catalogo.buscar(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));
        return evaluar(producto, perfil.obtener(usuarioId), cargarConocimiento());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ResultadoCompatibilidad> evaluarProductosPerfil(Long usuarioId, List<Long> productoIds) {
        // Perfil y base de conocimiento se cargan una sola vez para todo el lote.
        PerfilConsultaPort.PerfilConsulta datos = perfil.obtener(usuarioId);
        Conocimiento conocimiento = cargarConocimiento();
        Map<Long, CatalogoConsultaPort.ProductoConsulta> productos = catalogo.buscarVarios(new LinkedHashSet<>(productoIds));
        Map<Long, ResultadoCompatibilidad> resultados = new LinkedHashMap<>();
        for (Long id : new LinkedHashSet<>(productoIds)) {
            // Un id inexistente en el lote se omite: no debe tumbar la evaluación del resto.
            CatalogoConsultaPort.ProductoConsulta p = productos.get(id);
            if (p != null) resultados.put(id, evaluar(p, datos, conocimiento));
        }
        return resultados;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cruce> evaluarCruces(List<Long> productoIds) {
        List<ReglaIncompatibilidad> pares = reglas.reglasIncompatibilidad();
        Map<Long, CatalogoConsultaPort.ProductoConsulta> encontrados = catalogo.buscarVarios(new LinkedHashSet<>(productoIds));
        List<Long> ids = new ArrayList<>();
        List<CatalogoConsultaPort.ProductoConsulta> productos = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(productoIds)) {
            if (encontrados.containsKey(id)) { ids.add(id); productos.add(encontrados.get(id)); }
        }
        List<Cruce> cruces = new ArrayList<>();
        for (int i = 0; i < productos.size(); i++) {
            for (int j = i + 1; j < productos.size(); j++) {
                var a = productos.get(i);
                var b = productos.get(j);
                if (a.zona() != b.zona()) continue;
                Set<String> enA = Set.copyOf(a.ingredientesInci());
                Set<String> enB = Set.copyOf(b.ingredientesInci());
                // Una alerta por causa: retinol frente a un peeling con 4 ácidos es un solo choque, no cuatro.
                Map<String, List<Set<String>>> porMotivo = new LinkedHashMap<>();
                Map<String, Severidad> severidad = new LinkedHashMap<>();
                for (ReglaIncompatibilidad par : pares) {
                    // El par no es ordenado: el ingrediente A de la regla puede estar en cualquiera de los dos.
                    String x = par.getIngredienteA();
                    String y = par.getIngredienteB();
                    String deA = enA.contains(x) && enB.contains(y) ? x : enA.contains(y) && enB.contains(x) ? y : null;
                    if (deA == null) continue;
                    var lados = porMotivo.computeIfAbsent(par.getMotivo(), m -> List.of(new TreeSet<>(), new TreeSet<>()));
                    lados.get(0).add(deA);
                    lados.get(1).add(deA.equals(x) ? y : x);
                    severidad.merge(par.getMotivo(), par.getSeveridad(), (s1, s2) -> s1.compareTo(s2) >= 0 ? s1 : s2);
                }
                for (var e : porMotivo.entrySet()) {
                    cruces.add(new Cruce(ids.get(i), ids.get(j), new Alerta(severidad.get(e.getKey()),
                            "«" + a.nombre() + "» (" + String.join(", ", e.getValue().get(0)) + ") y «" + b.nombre()
                                    + "» (" + String.join(", ", e.getValue().get(1)) + ") no conviene usarlos al mismo tiempo.",
                            e.getKey())));
                }
            }
        }
        return cruces;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> filtrarCatalogo(Long usuarioId, String tipoPiel, boolean excluirAlergenos) {
        // Perfil "de filtro": solo lo que el cliente activó, así toda alerta viene de un filtro elegido
        // (piel grasa no oculta un producto por la rosácea del perfil, ni por alérgenos si no lo pidió).
        Set<String> alergias = excluirAlergenos ? perfil.obtener(usuarioId).alergias() : Set.of();
        Set<String> condiciones = tipoPiel == null ? Set.of() : Set.of(tipoPiel.toLowerCase());
        PerfilConsultaPort.PerfilConsulta filtro = new PerfilConsultaPort.PerfilConsulta(alergias, condiciones);
        Conocimiento conocimiento = cargarConocimiento();
        return catalogo.listarCatalogo().entrySet().stream()
                .filter(e -> !evaluar(e.getValue(), filtro, conocimiento).tieneAlertas())
                .map(Map.Entry::getKey)
                .toList();
    }

    private ResultadoCompatibilidad evaluar(CatalogoConsultaPort.ProductoConsulta producto,
                                            PerfilConsultaPort.PerfilConsulta datos, Conocimiento k) {
        Set<String> ingredientes = Set.copyOf(producto.ingredientesInci());
        ResultadoCompatibilidad resultado = ResultadoCompatibilidad.vacio();

        // 1) Alergias declaradas → siempre crítico (RNF-01: ningún falso negativo aquí). Cada
        //    alérgeno es un grupo (ej. parabenos) que se expande a todos sus nombres INCI.
        for (String alergeno : datos.alergias()) {
            for (String inci : k.porAlergeno().getOrDefault(alergeno, Set.of())) {
                if (ingredientes.contains(inci)) {
                    String grupo = datos.etiqueta(alergeno);
                    resultado.agregar(new Alerta(Severidad.ALTA,
                            "«" + producto.nombre() + "» contiene " + inci + " (" + grupo + "), al que registraste alergia.",
                            "Alergia declarada en tu perfil: " + grupo + ". " + inci + " pertenece a ese grupo."));
                }
            }
        }

        // 2) Irritantes conocidos para una condición del perfil (tipo de piel o afección).
        for (ReglaPerfil regla : k.reglasPerfil()) {
            if (ingredientes.contains(regla.getIngrediente()) && datos.condiciones().contains(regla.getCondicion())) {
                resultado.agregar(new Alerta(regla.getSeveridad(),
                        "«" + producto.nombre() + "» contiene " + regla.getIngrediente()
                                + ", no recomendado para tu condición: " + datos.etiqueta(regla.getCondicion()) + ".",
                        regla.getMotivo()));
            }
        }

        // 3) Rangos por condición (HU7): grado comedogénico de cada ingrediente y pH del producto.
        for (ReglaRango regla : k.rangos()) {
            if (!datos.condiciones().contains(regla.condicion())) continue;
            String condicion = datos.etiqueta(regla.condicion());
            if (regla.atributo() == ReglaRango.Atributo.COMEDOGENICIDAD) {
                for (String inci : ingredientes) {
                    Integer grado = k.comedogenicidad().get(inci);
                    if (grado != null && regla.contiene(grado)) {
                        resultado.agregar(new Alerta(regla.severidad(),
                                "«" + producto.nombre() + "» contiene " + inci + " (comedogenicidad " + grado
                                        + " de 5), poco recomendable para tu condición: " + condicion + ".",
                                regla.motivo()));
                    }
                }
            } else if (producto.ph() != null && regla.contiene(producto.ph())) {
                resultado.agregar(new Alerta(regla.severidad(),
                        "«" + producto.nombre() + "» tiene pH " + producto.ph()
                                + ", poco recomendable para tu condición: " + condicion + ".",
                        regla.motivo()));
            }
        }

        // 4) pH medido de la piel: si ya está por encima de lo sano, un producto alcalino lo empeora.
        if (datos.ph() != null && datos.ph() > PH_PIEL_MAXIMO && producto.ph() != null && producto.ph() >= PH_ALCALINO) {
            resultado.agregar(new Alerta(Severidad.MEDIA,
                    "«" + producto.nombre() + "» es alcalino (pH " + producto.ph() + ") y el pH de tu piel ya es alto ("
                            + datos.ph() + ").",
                    "La piel sana está entre pH 4.7 y 5.75; un pH más alto indica una barrera debilitada y los "
                            + "productos alcalinos la alteran más (Ali & Yosipovitch, 2013)."));
        }

        // 5) Ingredientes recomendados para una condición del perfil → base de la recomendación.
        for (Beneficio b : k.beneficios()) {
            if (ingredientes.contains(b.ingrediente()) && datos.condiciones().contains(b.condicion())) {
                resultado.agregarBeneficio("Contiene " + b.ingrediente() + ", recomendado para "
                        + datos.etiqueta(b.condicion()) + ": " + b.motivo());
            }
        }
        return resultado;
    }
}
