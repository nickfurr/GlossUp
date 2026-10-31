package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.port.out.DireccionRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarDireccionesServiceTest {

    @Mock private DireccionRepositoryPort repo;

    private GestionarDireccionesService service;

    @BeforeEach
    void setUp() {
        service = new GestionarDireccionesService(repo);
        lenient().when(repo.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static Direccion direccion(Long id, Long usuario, boolean principal) {
        return new Direccion(id, usuario, "Ana", "3001234567", "Quindío", "Armenia", "Centro", "Calle " + id, null, null, principal);
    }

    @Test
    void laPrimeraDireccionEsPrincipalAunqueNoSePida() {
        when(repo.listar(7L)).thenReturn(List.of());
        assertTrue(service.crear(direccion(null, 7L, false)).principal());
    }

    @Test
    void marcarOtraComoPrincipal_quitaLaAnterior() {
        when(repo.listar(7L)).thenReturn(List.of(direccion(1L, 7L, true)));
        assertTrue(service.crear(direccion(null, 7L, true)).principal());
        verify(repo).quitarPrincipal(7L);
    }

    @Test
    void agregarSinMarcar_noTocaLaPrincipal() {
        when(repo.listar(7L)).thenReturn(List.of(direccion(1L, 7L, true)));
        assertFalse(service.crear(direccion(null, 7L, false)).principal());
        verify(repo, never()).quitarPrincipal(any());
    }

    @Test
    void noPasaDelMaximoDeDirecciones() {
        when(repo.listar(7L)).thenReturn(Collections.nCopies(10, direccion(1L, 7L, false)));
        assertThrows(ReglaNegocioException.class, () -> service.crear(direccion(null, 7L, false)));
    }

    @Test
    void departamentoInventado_falla() {
        Direccion mala = new Direccion(null, 7L, "Ana", "3001234567", "Narnia", "X", "Y", "Z", null, null, false);
        assertThrows(ReglaNegocioException.class, () -> service.crear(mala));
    }

    @Test
    void editarLaPrincipal_laMantienePrincipal() {
        when(repo.buscar(1L)).thenReturn(Optional.of(direccion(1L, 7L, true)));
        assertTrue(service.actualizar(1L, direccion(null, 7L, false)).principal());
    }

    @Test
    void borrarLaPrincipal_pasaLaMarcaALaSiguiente() {
        when(repo.buscar(1L)).thenReturn(Optional.of(direccion(1L, 7L, true)));
        when(repo.listar(7L)).thenReturn(List.of(direccion(2L, 7L, false)));

        service.eliminar(7L, 1L);

        verify(repo).eliminar(1L);
        verify(repo).guardar(argThat(d -> d.id().equals(2L) && d.principal()));
    }

    @Test
    void noSeTocaLaDireccionDeOtroCliente() {
        when(repo.buscar(1L)).thenReturn(Optional.of(direccion(1L, 8L, false)));
        assertThrows(NotFoundException.class, () -> service.actualizar(1L, direccion(null, 7L, false)));
        assertThrows(NotFoundException.class, () -> service.eliminar(7L, 1L));
        verify(repo, never()).eliminar(any());
    }
}
