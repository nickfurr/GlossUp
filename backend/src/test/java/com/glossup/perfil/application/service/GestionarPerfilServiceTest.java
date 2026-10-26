package com.glossup.perfil.application.service;

import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarPerfilServiceTest {

    @Mock
    private PerfilRepositoryPort perfilRepository;

    private GestionarPerfilService service;

    @BeforeEach
    void setUp() {
        service = new GestionarPerfilService(perfilRepository);
    }

    @Test
    void obtenerPerfil_cuandoExiste_retornaPerfilExistenteSinGuardar() {
        PerfilDermatologico existente = new PerfilDermatologico(
                10L, 1L, new Ph(5.5), TipoPiel.MIXTA, TipoCabello.LISO_1A,
                Set.of(Afeccion.ACNE), Set.of(Alergeno.FRAGANCIA), OffsetDateTime.now()
        );
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));

        PerfilDermatologico resultado = service.obtenerPerfil(1L);

        assertEquals(10L, resultado.getId());
        assertTrue(resultado.esAlergicoA(Alergeno.FRAGANCIA));
        verify(perfilRepository, never()).guardar(any());
    }

    @Test
    void obtenerPerfil_cuandoNoExiste_creaYPersistePerfilInicialVacio() {
        when(perfilRepository.buscarPorUsuarioId(2L)).thenReturn(Optional.empty());
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> {
                    PerfilDermatologico p = inv.getArgument(0);
                    return new PerfilDermatologico(11L, p.getUsuarioId(), p.getPh(), p.getTipoPiel(),
                            p.getTipoCabello(), p.getAfecciones(), p.getAlergias(), p.getActualizadoEn());
                });

        PerfilDermatologico resultado = service.obtenerPerfil(2L);

        assertEquals(11L, resultado.getId());
        assertEquals(2L, resultado.getUsuarioId());
        assertTrue(resultado.getAlergias().isEmpty());
        assertTrue(resultado.getAfecciones().isEmpty());
        verify(perfilRepository).guardar(any(PerfilDermatologico.class));
    }

    @Test
    void agregarAlergia_aceptaCodigoDelCatalogoSinImportarMayusculasYPersiste() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, new Ph(5.2), TipoPiel.GRASA, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico resultado = service.agregarAlergia(1L, "  parabenos ");

        assertTrue(resultado.esAlergicoA(Alergeno.PARABENOS));
        assertEquals(1, resultado.getAlergias().size());
        verify(perfilRepository).guardar(any(PerfilDermatologico.class));
    }

    @Test
    void quitarAlergia_eliminaIngredienteNormalizadoYPersiste() {
        PerfilDermatologico existente = new PerfilDermatologico(
                10L, 1L, new Ph(5.2), TipoPiel.SECA, null,
                Set.of(), Set.of(Alergeno.FRAGANCIA, Alergeno.LINALOOL), OffsetDateTime.now()
        );
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico resultado = service.quitarAlergia(1L, "FRAGANCIA");

        assertFalse(resultado.esAlergicoA(Alergeno.FRAGANCIA));
        assertTrue(resultado.esAlergicoA(Alergeno.LINALOOL));
        assertEquals(1, resultado.getAlergias().size());
    }

    @Test
    void agregarYQuitarAfeccion_actualizaCondicionesYPersiste() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, null, TipoPiel.SENSIBLE, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico conAfeccion = service.agregarAfeccion(1L, "DERMATITIS_ATOPICA");
        assertTrue(conAfeccion.tieneAfeccion(Afeccion.DERMATITIS_ATOPICA));

        PerfilDermatologico sinAfeccion = service.quitarAfeccion(1L, "dermatitis_atopica");
        assertFalse(sinAfeccion.tieneAfeccion(Afeccion.DERMATITIS_ATOPICA));
    }

    @Test
    void agregarAlergiaVacia_lanzaReglaNegocioExceptionSinGuardar() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, null, null, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));

        assertThrows(ReglaNegocioException.class, () -> service.agregarAlergia(1L, "   "));
        verify(perfilRepository, never()).guardar(any());
    }

    @Test
    void textoLibreFueraDelCatalogo_lanzaReglaNegocioExceptionSinGuardar() {
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(PerfilDermatologico.crear(1L, null, null, null)));

        assertThrows(ReglaNegocioException.class, () -> service.agregarAlergia(1L, "polen de mi jardín"));
        assertThrows(ReglaNegocioException.class, () -> service.agregarAfeccion(1L, "me pica"));
        assertThrows(ReglaNegocioException.class, () -> service.actualizarBiotipo(1L, null, null, "rizadito"));
        verify(perfilRepository, never()).guardar(any());
    }

    @Test
    void actualizarBiotipo_conCodigosValidos_guardaEnums() {
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(PerfilDermatologico.crear(1L, null, null, null)));
        when(perfilRepository.guardar(any(PerfilDermatologico.class))).thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico resultado = service.actualizarBiotipo(1L, 5.0, "sensible", "RIZADO_3B");

        assertEquals(TipoPiel.SENSIBLE, resultado.getTipoPiel());
        assertEquals(TipoCabello.RIZADO_3B, resultado.getTipoCabello());
    }
}
