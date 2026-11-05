package com.glossup.recomendacion.domain.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recomendador híbrido (auditoría NC-T10 / R-06). La seguridad la deciden las reglas del motor de compatibilidad:
 * con perfil, nunca recomienda un producto con alertas para él. Entre los aptos ordena por cuatro señales:
 * <ul>
 *   <li>perfil: ingredientes recomendados para las condiciones del cliente (base de conocimiento);</li>
 *   <li>contenido: fórmula parecida (Jaccard de ingredientes INCI) a lo que el cliente ya compró;</li>
 *   <li>colaborativa: lo que otros clientes compraron en el mismo pedido que eso (filtrado colaborativo por ítems);</li>
 *   <li>popularidad: en cuántos pedidos pagados aparece.</li>
 * </ul>
 * Cada recomendación explica su razón principal. Es determinista: mismos datos, mismo resultado.
 */
public final class MotorRecomendacion {

    static final double PESO_PERFIL = 0.5;
    static final double PESO_CONTENIDO = 0.2;
    static final double PESO_COLABORATIVO = 0.2;
    static final double PESO_POPULARIDAD = 0.1;
    /** Variedad: no más de dos de la misma categoría mientras haya otras opciones. */
    static final int MAX_POR_CATEGORIA = 2;
    /** Con tres beneficios para el perfil la señal ya es máxima. */
    private static final double BENEFICIOS_PARA_MAXIMO = 3.0;

    /** Producto publicado que se puede recomendar. */
    public record Candidato(Long id, String nombre, String categoria, Set<String> ingredientes, OffsetDateTime creadoEn) {}

    /** Lo que dijo el motor de compatibilidad del producto frente al perfil del cliente. */
    public record Calificacion(boolean compatible, boolean conAlertas, List<String> beneficios) {}

    /** Productos de un pedido pagado y de quién es. */
    public record Compra(Long usuarioId, Set<Long> productos) {}

    private MotorRecomendacion() {}

    /**
     * @param calificaciones por id de producto; se ignora si {@code perfilVacio}
     * @param compras        pedidos pagados de todos los clientes, incluido este
     */
    public static List<Recomendacion> recomendar(Long usuarioId, List<Candidato> candidatos, boolean perfilVacio,
                                                 Map<Long, Calificacion> calificaciones, List<Compra> compras, int limite) {
        Map<Long, Candidato> porId = new HashMap<>();
        candidatos.forEach(c -> porId.put(c.id(), c));

        Set<Long> comprados = new HashSet<>();
        compras.stream().filter(c -> c.usuarioId().equals(usuarioId)).forEach(c -> comprados.addAll(c.productos()));

        Map<Long, Integer> popularidad = new HashMap<>();
        // coCompra.get(candidato).get(comprado): pedidos de otros clientes con ambos productos.
        Map<Long, Map<Long, Integer>> coCompra = new HashMap<>();
        for (Compra compra : compras) {
            compra.productos().forEach(p -> popularidad.merge(p, 1, Integer::sum));
            if (compra.usuarioId().equals(usuarioId)) continue;
            for (Long comun : compra.productos()) {
                if (!comprados.contains(comun)) continue;
                for (Long otro : compra.productos()) {
                    if (!otro.equals(comun)) coCompra.computeIfAbsent(otro, k -> new HashMap<>()).merge(comun, 1, Integer::sum);
                }
            }
        }
        int maxPopularidad = popularidad.values().stream().max(Integer::compare).orElse(0);
        int maxCoCompra = coCompra.values().stream().mapToInt(MotorRecomendacion::total).max().orElse(0);

        List<Puntuado> puntuados = new ArrayList<>();
        for (Candidato c : candidatos) {
            if (comprados.contains(c.id())) continue; // lo ya comprado está en "Volver a comprar"
            Calificacion cal = calificaciones.get(c.id());
            if (!perfilVacio && (cal == null || !cal.compatible() || cal.conAlertas())) continue;

            List<String> beneficios = perfilVacio || cal == null ? List.of() : cal.beneficios();
            double perfil = Math.min(1.0, beneficios.size() / BENEFICIOS_PARA_MAXIMO);

            Candidato parecido = null;
            double contenido = 0;
            for (Long id : comprados) {
                Candidato comprado = porId.get(id);
                if (comprado == null) continue;
                double s = jaccard(c.ingredientes(), comprado.ingredientes());
                if (s > contenido) {
                    contenido = s;
                    parecido = comprado;
                }
            }

            Map<Long, Integer> conjuntos = coCompra.getOrDefault(c.id(), Map.of());
            double colaborativo = maxCoCompra == 0 ? 0 : (double) total(conjuntos) / maxCoCompra;
            Candidato companero = conjuntos.entrySet().stream()
                    .max(Map.Entry.<Long, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .map(e -> porId.get(e.getKey())).orElse(null);

            double popular = maxPopularidad == 0 ? 0 : (double) popularidad.getOrDefault(c.id(), 0) / maxPopularidad;

            double score = PESO_PERFIL * perfil + PESO_CONTENIDO * contenido + PESO_COLABORATIVO * colaborativo
                    + PESO_POPULARIDAD * popular;
            String razon = razon(perfilVacio, beneficios, PESO_PERFIL * perfil, parecido, PESO_CONTENIDO * contenido,
                    companero, PESO_COLABORATIVO * colaborativo, PESO_POPULARIDAD * popular);
            puntuados.add(new Puntuado(c, Math.min(1.0, score), razon));
        }

        puntuados.sort(Comparator.comparingDouble(Puntuado::score).reversed()
                .thenComparing(p -> p.candidato().creadoEn(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(p -> p.candidato().id()));
        return conVariedad(puntuados, limite).stream()
                .map(p -> new Recomendacion(p.candidato().id(), p.razon(), p.score()))
                .toList();
    }

    private record Puntuado(Candidato candidato, double score, String razon) {}

    /** La señal que más aportó al puntaje es la que se le explica al cliente. */
    private static String razon(boolean perfilVacio, List<String> beneficios, double aportePerfil,
                                Candidato parecido, double aporteContenido,
                                Candidato companero, double aporteColaborativo, double aportePopularidad) {
        double mayor = Math.max(Math.max(aportePerfil, aporteContenido), Math.max(aporteColaborativo, aportePopularidad));
        if (mayor == 0) {
            return perfilVacio ? "Nuevo en el catálogo" : "Compatible con tu perfil: ninguno de sus ingredientes te genera alertas";
        }
        if (mayor == aportePerfil) return beneficios.get(0);
        if (mayor == aporteContenido) return "Tiene una fórmula parecida a " + parecido.nombre() + ", que ya compraste";
        if (mayor == aporteColaborativo) return "Quienes compraron " + companero.nombre() + " también lo llevaron";
        return "De los más comprados en GlossUP";
    }

    private static List<Puntuado> conVariedad(List<Puntuado> ordenados, int limite) {
        List<Puntuado> elegidos = new ArrayList<>();
        Map<String, Integer> porCategoria = new HashMap<>();
        for (Puntuado p : ordenados) {
            if (elegidos.size() == limite) break;
            String cat = p.candidato().categoria() == null ? "" : p.candidato().categoria();
            if (porCategoria.getOrDefault(cat, 0) < MAX_POR_CATEGORIA) {
                elegidos.add(p);
                porCategoria.merge(cat, 1, Integer::sum);
            }
        }
        for (Puntuado p : ordenados) { // pocas categorías: se completa con los mejores restantes
            if (elegidos.size() == limite) break;
            if (!elegidos.contains(p)) elegidos.add(p);
        }
        elegidos.sort(Comparator.comparingDouble(Puntuado::score).reversed()
                .thenComparing(p -> p.candidato().creadoEn(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(p -> p.candidato().id()));
        return elegidos;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> comun = new HashSet<>(a);
        comun.retainAll(b);
        return (double) comun.size() / (a.size() + b.size() - comun.size());
    }

    private static int total(Map<Long, Integer> conteos) {
        return conteos.values().stream().mapToInt(Integer::intValue).sum();
    }
}
