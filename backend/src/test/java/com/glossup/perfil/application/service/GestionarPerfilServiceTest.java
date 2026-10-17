package com.glossup.perfil.application.service;

import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
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
                10L, 1L, new Ph(5.5), TipoPiel.MIXTA, "liso",
                Set.of("acne"), Set.of("parfum"), OffsetDateTime.now()
        );
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));

        PerfilDermatologico resultado = service.obtenerPerfil(1L);

        assertEquals(10L, resultado.getId());
        assertTrue(resultado.esAlergicoA("parfum"));
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
    void agregarAlergia_normalizaYPersisteSinDuplicados() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, new Ph(5.2), TipoPiel.GRASA, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico resultado = service.agregarAlergia(1L, "  Sodium Lauryl Sulfate ");

        assertTrue(resultado.esAlergicoA("sodium lauryl sulfate"));
        assertEquals(1, resultado.getAlergias().size());
        verify(perfilRepository).guardar(any(PerfilDermatologico.class));
    }

    @Test
    void quitarAlergia_eliminaIngredienteNormalizadoYPersiste() {
        PerfilDermatologico existente = new PerfilDermatologico(
                10L, 1L, new Ph(5.2), TipoPiel.SECA, null,
                Set.of(), Set.of("parfum", "linalool"), OffsetDateTime.now()
        );
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico resultado = service.quitarAlergia(1L, "PARFUM");

        assertFalse(resultado.esAlergicoA("parfum"));
        assertTrue(resultado.esAlergicoA("linalool"));
        assertEquals(1, resultado.getAlergias().size());
    }

    @Test
    void agregarYQuitarAfeccion_actualizaCondicionesYPersiste() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, null, TipoPiel.SENSIBLE, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));
        when(perfilRepository.guardar(any(PerfilDermatologico.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PerfilDermatologico conAfeccion = service.agregarAfeccion(1L, "Dermatitis Atópica");
        assertTrue(conAfeccion.tieneAfeccion("dermatitis atópica"));

        PerfilDermatologico sinAfeccion = service.quitarAfeccion(1L, "DERMATITIS ATÓPICA");
        assertFalse(sinAfeccion.tieneAfeccion("dermatitis atópica"));
    }

    @Test
    void agregarAlergiaVacia_lanzaReglaNegocioExceptionSinGuardar() {
        PerfilDermatologico existente = PerfilDermatologico.crear(1L, null, null, null);
        when(perfilRepository.buscarPorUsuarioId(1L)).thenReturn(Optional.of(existente));

        assertThrows(ReglaNegocioException.class, () -> service.agregarAlergia(1L, "   "));
        verify(perfilRepository, never()).guardar(any());
    }
}
