package com.glossup.compatibilidad.application.service;

import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort.ProductoConsulta;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort.PerfilConsulta;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
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
    }

    private void producto(String... inci) {
        when(catalogo.obtener(10L)).thenReturn(new ProductoConsulta("Serum X", List.of(inci)));
    }

    private void perfilCon(Set<String> alergias, Set<String> condiciones) {
        when(perfil.obtener(1L)).thenReturn(new PerfilConsulta(alergias, condiciones));
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
}
