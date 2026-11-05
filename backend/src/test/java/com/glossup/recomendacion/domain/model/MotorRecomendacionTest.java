package com.glossup.recomendacion.domain.model;

import com.glossup.recomendacion.domain.model.MotorRecomendacion.Calificacion;
import com.glossup.recomendacion.domain.model.MotorRecomendacion.Candidato;
import com.glossup.recomendacion.domain.model.MotorRecomendacion.Compra;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MotorRecomendacionTest {

    private static final Long ANA = 1L;
    private static final OffsetDateTime HOY = OffsetDateTime.parse("2026-11-05T12:00:00Z");

    private static Candidato producto(long id, String categoria, String... inci) {
        return new Candidato(id, "Producto " + id, categoria, Set.of(inci), HOY.minusDays(id));
    }

    private static Calificacion apto(String... beneficios) {
        return new Calificacion(true, false, List.of(beneficios));
    }

    private static List<Long> ids(List<Recomendacion> rs) {
        return rs.stream().map(Recomendacion::productoId).toList();
    }

    @Test
    void conPerfil_nuncaRecomiendaUnProductoConAlertasNiIncompatible() {
        List<Candidato> c = List.of(producto(1, "Sérums", "niacinamide"), producto(2, "Sérums", "parfum"), producto(3, "Tónicos", "alcohol"));
        Map<Long, Calificacion> cal = Map.of(
                1L, apto(),
                2L, new Calificacion(true, true, List.of("Contiene niacinamide, recomendado para acné")), // precaución
                3L, new Calificacion(false, true, List.of()));                                          // no compatible
        assertEquals(List.of(1L), ids(MotorRecomendacion.recomendar(ANA, c, false, cal, List.of(), 6)));
    }

    @Test
    void losBeneficiosParaElPerfilPesanMasYSonLaRazon() {
        List<Candidato> c = List.of(producto(1, "Sérums", "aqua"), producto(2, "Cremas", "niacinamide", "zinc pca"));
        Map<Long, Calificacion> cal = Map.of(1L, apto(), 2L, apto("Contiene niacinamide, recomendado para acné"));
        List<Recomendacion> rs = MotorRecomendacion.recomendar(ANA, c, false, cal, List.of(), 6);
        assertEquals(List.of(2L, 1L), ids(rs));
        assertEquals("Contiene niacinamide, recomendado para acné", rs.get(0).razon());
        assertTrue(rs.get(0).score() > rs.get(1).score());
    }

    @Test
    void sinPerfil_usaLasComprasDeOtrosYLoYaCompradoNoSeRepite() {
        List<Candidato> c = List.of(producto(1, "Limpiadores", "aqua", "glycerin"), producto(2, "Cremas", "aqua", "glycerin", "urea"),
                producto(3, "Tónicos", "aqua"), producto(4, "Sérums", "retinol"));
        List<Compra> compras = List.of(
                new Compra(ANA, Set.of(1L)),
                new Compra(2L, Set.of(1L, 4L)),   // otro cliente compró 1 junto con 4
                new Compra(3L, Set.of(1L, 4L)),
                new Compra(4L, Set.of(3L)));
        List<Recomendacion> rs = MotorRecomendacion.recomendar(ANA, c, true, Map.of(), compras, 6);

        assertFalse(ids(rs).contains(1L), "ya lo compró");
        assertEquals(4L, rs.get(0).productoId());
        assertEquals("Quienes compraron Producto 1 también lo llevaron", rs.get(0).razon());
        Recomendacion parecido = rs.stream().filter(r -> r.productoId() == 2L).findFirst().orElseThrow();
        assertEquals("Tiene una fórmula parecida a Producto 1, que ya compraste", parecido.razon());
    }

    @Test
    void variedad_noMasDeDosPorCategoriaSiHayOtras() {
        List<Candidato> c = List.of(producto(1, "Sérums", "a"), producto(2, "Sérums", "b"), producto(3, "Sérums", "c"),
                producto(4, "Cremas", "d"));
        Map<Long, Calificacion> cal = Map.of(1L, apto("x", "y", "z"), 2L, apto("x", "y"), 3L, apto("x"), 4L, apto());
        List<Long> top3 = ids(MotorRecomendacion.recomendar(ANA, c, false, cal, List.of(), 3));
        assertEquals(List.of(1L, 2L, 4L), top3);
        // Con más cupo se completa con el tercer sérum.
        assertEquals(4, MotorRecomendacion.recomendar(ANA, c, false, cal, List.of(), 4).size());
    }

    @Test
    void sinNingunaSenal_ordenaPorLoMasNuevo() {
        List<Candidato> c = List.of(producto(5, "A", "a"), producto(2, "B", "b"));
        List<Recomendacion> rs = MotorRecomendacion.recomendar(ANA, c, true, Map.of(), List.of(), 6);
        assertEquals(List.of(2L, 5L), ids(rs));
        assertEquals("Nuevo en el catálogo", rs.get(0).razon());
        assertTrue(rs.stream().allMatch(r -> r.score() >= 0 && r.score() <= 1));
    }

    @Test
    void jaccard() {
        assertEquals(0.5, MotorRecomendacion.jaccard(Set.of("a", "b"), Set.of("b", "c", "a", "d")), 1e-9);
        assertEquals(0.0, MotorRecomendacion.jaccard(Set.of(), Set.of("a")));
    }
}
