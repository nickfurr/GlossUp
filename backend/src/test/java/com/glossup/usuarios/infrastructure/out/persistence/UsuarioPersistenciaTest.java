package com.glossup.usuarios.infrastructure.out.persistence;

import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.glossup.usuarios.infrastructure.out.persistence.adapter.SesionVigenteAdapter;
import com.glossup.usuarios.infrastructure.out.persistence.adapter.UsuarioPersistenceAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El adaptador reconstruye la entidad desde cero en cada guardado: todo campo del dominio debe ir y volver de la
 * base (aprobación, política, suspensión, sesiones). Contra PostgreSQL real con todas las migraciones.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UsuarioPersistenceAdapter.class, SesionVigenteAdapter.class})
class UsuarioPersistenciaTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired UsuarioRepositoryPort usuarios;
    @Autowired SesionVigenteAdapter sesiones;
    @Autowired EntityManager em;

    @Test
    void todoElEstadoDeLaCuentaSobreviveAlGuardar() {
        Usuario nuevo = Usuario.registrar("tienda@glossup.co", "hash", "Tienda", Rol.VENDEDOR);
        OffsetDateTime acepto = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        nuevo.aceptarPolitica("1.0", acepto);
        nuevo.quedarPendienteDeAprobacion();
        Usuario guardado = usuarios.guardar(nuevo);

        Usuario leido = usuarios.buscarPorEmail("TIENDA@glossup.co").orElseThrow();
        assertEquals(guardado.getId(), leido.getId());
        assertTrue(leido.isPendienteAprobacion());
        assertTrue(leido.tieneRol(Rol.VENDEDOR));
        assertEquals("1.0", leido.getPoliticaVersion());
        assertTrue(acepto.isEqual(leido.getPoliticaAceptadaEn()));

        leido.aprobar();
        leido.suspender("Prueba");
        usuarios.guardar(leido);
        Usuario otraVez = usuarios.buscarPorId(leido.getId()).orElseThrow();
        assertFalse(otraVez.isPendienteAprobacion());
        assertFalse(otraVez.isActivo());
        assertEquals("Prueba", otraVez.getMotivoSuspension());
        assertTrue(usuarios.listarTodos().stream().anyMatch(u -> u.getId().equals(otraVez.getId())));
    }

    @Test
    void cerrarSesionesInvalidaLosTokensAnteriores() {
        Usuario ana = usuarios.guardar(Usuario.registrar("ana@glossup.co", "hash", "Ana", Rol.CLIENTE));
        String id = ana.getId().toString();
        Instant antes = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        assertTrue(sesiones.vigente(id, antes));

        ana.cerrarSesiones(OffsetDateTime.now());
        usuarios.guardar(ana);
        em.flush(); // la consulta de sesiones va por JDBC: en producción ya vería el commit de la petición anterior
        assertFalse(sesiones.vigente(id, antes), "un token de antes ya no vale");
        assertTrue(sesiones.vigente(id, Instant.now().plusSeconds(1)), "uno emitido después sí");
        assertFalse(sesiones.vigente("no-numerico", antes));
        assertFalse(sesiones.vigente("999999", antes), "cuenta inexistente");
    }
}
