package com.glossup.compatibilidad.application.service;

import com.glossup.compatibilidad.domain.model.Beneficio;
import com.glossup.compatibilidad.domain.model.Cruce;
import com.glossup.compatibilidad.domain.model.ReglaIncompatibilidad;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ReglaRango;
import com.glossup.compatibilidad.domain.model.ReglaRango.Atributo;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort.ProductoConsulta;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort.PerfilConsulta;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluarCompatibilidadServiceTest {

    @Mock private PerfilConsultaPort perfil;
    @Mock private CatalogoConsultaPort catalogo;
    @Mock private ReglaRepositoryPort reglas;

    private EvaluarCompatibilidadService service;

    @BeforeEach
    void setUp() {
        service = new EvaluarCompatibilidadService(perfil, catalogo, reglas);
        lenient().when(reglas.reglasPerfil()).thenReturn(List.of(
                new ReglaPerfil(1L, "retinol", "sensible", Severidad.ALTA, "Irrita piel sensible")));
        lenient().when(reglas.ingredientesPorAlergeno()).thenReturn(Map.of(
                "parabenos", Set.of("methylparaben", "propylparaben"),
                "fragancia", Set.of("parfum", "linalool")));
        lenient().when(reglas.beneficios()).thenReturn(List.of(
                new Beneficio("niacinamide", "acne", "Antiinflamatorio y seborregulador.")));
        lenient().when(reglas.comedogenicidad()).thenReturn(Map.of(
                "isopropyl myristate", 5, "tocopherol", 3, "glycerin", 0));
        lenient().when(reglas.reglasRango()).thenReturn(List.of(
                new ReglaRango(Atributo.COMEDOGENICIDAD, "acne", 4, 5, Severidad.MEDIA, "Comedogénico alto"),
                new ReglaRango(Atributo.COMEDOGENICIDAD, "acne", 3, 3, Severidad.BAJA, "Comedogénico moderado"),
                new ReglaRango(Atributo.COMEDOGENICIDAD, "mixta", 4, 5, Severidad.BAJA, "Zona T"),
                new ReglaRango(Atributo.PH, "sensible", 0, 3.4, Severidad.MEDIA, "Muy ácido"),
                new ReglaRango(Atributo.PH, "seca", 7.5, 14, Severidad.MEDIA, "Alcalino")));
    }

    private void productoConPh(Double ph, String... inci) {
        when(catalogo.buscar(10L)).thenReturn(Optional.of(new ProductoConsulta("Serum X", List.of(inci), ph)));
    }

    private void producto(String... inci) {
        when(catalogo.buscar(10L)).thenReturn(Optional.of(new ProductoConsulta("Serum X", List.of(inci))));
    }

    private void perfilCon(Set<String> alergias, Set<String> condiciones) {
        when(perfil.obtener(1L)).thenReturn(new PerfilConsulta(alergias, condiciones));
    }

    @Test
    void cruces_detectaElParEnCualquierOrdenYSoloEntreProductosDistintos() {
        when(reglas.reglasIncompatibilidad()).thenReturn(List.of(
                new ReglaIncompatibilidad(1L, "retinol", "glycolic acid", Severidad.MEDIA, "Suman irritación")));
        when(catalogo.buscar(1L)).thenReturn(Optional.of(new ProductoConsulta("Tónico AHA", List.of("aqua", "glycolic acid"))));
        when(catalogo.buscar(2L)).thenReturn(Optional.of(new ProductoConsulta("Sérum Retinol", List.of("retinol"))));
        // Un producto que trae los dos ingredientes ya viene formulado así: no es un cruce.
        when(catalogo.buscar(3L)).thenReturn(Optional.of(new ProductoConsulta("Kit", List.of("retinol", "glycolic acid"))));
        when(catalogo.buscar(4L)).thenReturn(Optional.of(new ProductoConsulta("Crema", List.of("glycerin"))));
        when(catalogo.buscar(99L)).thenReturn(Optional.empty());

        List<Cruce> cruces = service.evaluarCruces(List.of(1L, 2L, 4L, 2L, 99L));
        assertEquals(1, cruces.size(), "ids repetidos o inexistentes no duplican ni rompen");
        Cruce c = cruces.get(0);
        assertEquals(List.of(1L, 2L), List.of(c.productoA(), c.productoB()));
        assertTrue(c.alerta().mensaje().contains("«Tónico AHA» (glycolic acid) y «Sérum Retinol» (retinol)"));
        assertEquals("Suman irritación", c.alerta().causa());

        assertTrue(service.evaluarCruces(List.of(3L, 4L)).isEmpty());
        assertEquals(3, service.evaluarCruces(List.of(1L, 2L, 3L)).size(), "los tres pares chocan: tónico-sérum, tónico-kit y sérum-kit");

        // Mismo par de ingredientes, pero uno va en el cabello: no se usan juntos, no hay cruce.
        when(catalogo.buscar(5L)).thenReturn(Optional.of(new ProductoConsulta("Acondicionador", List.of("glycolic acid"),
                null, com.glossup.compatibilidad.domain.model.Zona.CABELLO)));
        assertTrue(service.evaluarCruces(List.of(2L, 5L)).isEmpty());
    }

    @Test
    void cruces_variosIngredientesConLaMismaCausaSonUnaSolaAlerta() {
        when(reglas.reglasIncompatibilidad()).thenReturn(List.of(
                new ReglaIncompatibilidad(1L, "retinol", "glycolic acid", Severidad.MEDIA, "Retinoide + ácido"),
                new ReglaIncompatibilidad(2L, "retinol", "lactic acid", Severidad.MEDIA, "Retinoide + ácido"),
                new ReglaIncompatibilidad(3L, "benzoyl peroxide", "retinol", Severidad.MEDIA, "Oxida el retinol")));
        when(catalogo.buscar(1L)).thenReturn(Optional.of(new ProductoConsulta("Sérum", List.of("retinol"))));
        when(catalogo.buscar(2L)).thenReturn(Optional.of(new ProductoConsulta("Peeling",
                List.of("lactic acid", "glycolic acid", "benzoyl peroxide"))));

        List<Cruce> cruces = service.evaluarCruces(List.of(1L, 2L));
        assertEquals(2, cruces.size(), "una alerta por causa distinta");
        assertTrue(cruces.get(0).alerta().mensaje().contains("«Peeling» (glycolic acid, lactic acid)"));
    }

    @Test
    void comedogenicidad_laSeveridadDependeDelGradoYDeLaCondicion() {
        producto("isopropyl myristate", "tocopherol", "glycerin");
        perfilCon(Set.of(), Set.of("acne"));
        ResultadoCompatibilidad acne = service.evaluarProductoPerfil(1L, 10L);
        assertEquals(List.of(Severidad.MEDIA, Severidad.BAJA),
                acne.getAlertas().stream().map(a -> a.severidad()).sorted().toList().reversed());
        assertTrue(acne.esCompatible(), "la comedogenia avisa pero nunca bloquea");
        assertTrue(acne.getAlertas().stream().anyMatch(a -> a.mensaje().contains("comedogenicidad 5 de 5")));

        perfilCon(Set.of(), Set.of("mixta"));
        ResultadoCompatibilidad mixta = service.evaluarProductoPerfil(1L, 10L);
        assertEquals(1, mixta.getAlertas().size(), "en piel mixta solo alerta el grado 4–5");
        assertEquals(Severidad.BAJA, mixta.getAlertas().get(0).severidad());

        perfilCon(Set.of(), Set.of("normal"));
        assertFalse(service.evaluarProductoPerfil(1L, 10L).tieneAlertas());
    }

    @Test
    void ph_alertaFueraDelRangoToleradoPorLaCondicion() {
        productoConPh(3.0, "glycolic acid");
        perfilCon(Set.of(), Set.of("sensible"));
        ResultadoCompatibilidad acido = service.evaluarProductoPerfil(1L, 10L);
        assertEquals(1, acido.getAlertas().size());
        assertTrue(acido.getAlertas().get(0).mensaje().contains("pH 3.0"));

        productoConPh(3.5, "glycolic acid");
        assertFalse(service.evaluarProductoPerfil(1L, 10L).tieneAlertas(), "3.5 ya es tolerable (límite del CIR)");

        productoConPh(null, "glycolic acid");
        assertFalse(service.evaluarProductoPerfil(1L, 10L).tieneAlertas(), "sin pH declarado no se evalúa");
    }

    @Test
    void phDeLaPiel_altoMasProductoAlcalinoAlerta() {
        productoConPh(9.0, "sodium stearate");
        when(perfil.obtener(1L)).thenReturn(new PerfilConsulta(Set.of(), Set.of(), Map.of(), 6.2));
        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);
        assertEquals(1, r.getAlertas().size());
        assertEquals(Severidad.MEDIA, r.getAlertas().get(0).severidad());

        when(perfil.obtener(1L)).thenReturn(new PerfilConsulta(Set.of(), Set.of(), Map.of(), 5.2));
        assertFalse(service.evaluarProductoPerfil(1L, 10L).tieneAlertas(), "piel con pH sano no recibe aviso");
    }

    @Test
    void beneficio_soloRecomiendaSiNoHayNingunaAlerta() {
        producto("niacinamide");
        perfilCon(Set.of(), Set.of("acne"));
        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);
        assertTrue(r.esRecomendado());
        assertTrue(r.getBeneficios().get(0).contains("niacinamide"));

        producto("niacinamide", "retinol");
        perfilCon(Set.of(), Set.of("acne", "sensible"));
        ResultadoCompatibilidad conAlerta = service.evaluarProductoPerfil(1L, 10L);
        assertFalse(conAlerta.esRecomendado(), "un producto con alertas nunca se recomienda");
        assertEquals(1, conAlerta.getBeneficios().size());
    }

    @Test
    void alergeno_grupoDetectaCualquieraDeSusIngredientes() {
        producto("niacinamide", "propylparaben");
        perfilCon(Set.of("parabenos"), Set.of());

        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);

        assertFalse(r.esCompatible(), "un alérgeno presente nunca debe considerarse compatible (RNF-01)");
        assertEquals(1, r.getAlertas().size());
        assertEquals(Severidad.ALTA, r.getAlertas().get(0).severidad());
        assertTrue(r.getAlertas().get(0).causa().contains("propylparaben"));
    }

    @Test
    void irritante_cruzaIngredienteConCondicionDelPerfil() {
        producto("retinol");
        perfilCon(Set.of(), Set.of("sensible"));

        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);

        assertFalse(r.esCompatible());
        assertEquals(1, r.getAlertas().size());
        assertEquals(Severidad.ALTA, r.getAlertas().get(0).severidad());
    }

    @Test
    void sinCoincidencias_esCompatibleSinAlertas() {
        producto("retinol");
        perfilCon(Set.of("fragancia"), Set.of("grasa")); // retinol no es fragancia y su regla es para 'sensible'

        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);

        assertTrue(r.esCompatible());
        assertFalse(r.tieneAlertas());
    }

    @Test
    void perfilSinDatos_noFallaYEsCompatible() {
        producto("retinol", "parfum");
        perfilCon(Set.of(), Set.of());

        ResultadoCompatibilidad r = service.evaluarProductoPerfil(1L, 10L);

        assertTrue(r.esCompatible());
        assertFalse(r.tieneAlertas());
    }

    @Test
    void productoInexistente_lanzaNotFound() {
        when(catalogo.buscar(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.evaluarProductoPerfil(1L, 99L));
    }

    @Test
    void lote_evaluaCadaProductoOmiteInexistentesYCargaReglasUnaVez() {
        perfilCon(Set.of("parabenos"), Set.of("sensible"));
        when(catalogo.buscar(1L)).thenReturn(Optional.of(new ProductoConsulta("Crema", List.of("glycerin"))));
        when(catalogo.buscar(2L)).thenReturn(Optional.of(new ProductoConsulta("Serum", List.of("retinol", "methylparaben"))));
        when(catalogo.buscar(3L)).thenReturn(Optional.empty());

        Map<Long, ResultadoCompatibilidad> r = service.evaluarProductosPerfil(1L, List.of(1L, 2L, 3L, 2L));

        assertEquals(List.of(1L, 2L), List.copyOf(r.keySet()));
        assertTrue(r.get(1L).esCompatible());
        assertFalse(r.get(2L).esCompatible());
        assertEquals(2, r.get(2L).getAlertas().size()); // alergia a parabenos + retinol en piel sensible
        verify(reglas, times(1)).reglasPerfil();
        verify(perfil, times(1)).obtener(1L);
    }
}
