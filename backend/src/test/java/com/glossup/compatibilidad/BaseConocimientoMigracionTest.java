package com.glossup.compatibilidad;

import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.TipoPiel;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre todas las migraciones contra un PostgreSQL real y verifica que la base de conocimiento
 * del motor esté completa y coherente con los enums del dominio. Un hueco aquí sería un falso
 * negativo silencioso (RNF-01), por eso se prueba contra la BD y no con mocks.
 */
@Testcontainers(disabledWithoutDocker = true)
class BaseConocimientoMigracionTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrar() throws SQLException {
        // Estado previo a V5: perfiles con texto libre, como los guardaba la versión anterior.
        flyway("4").migrate();
        ejecutar("""
                INSERT INTO usuario (id, email, password_hash, nombre) VALUES (1, 'a@a.co', 'x', 'Ana'),
                       (2, 'manuel.pinedave@uqvirtual.edu.co', 'x', 'Vendedor demo');
                INSERT INTO perfil_dermatologico (id, usuario_id, tipo_piel, tipo_cabello) VALUES (1, 1, 'SENSIBLE', 'rizadito');
                INSERT INTO perfil_alergia (perfil_id, nombre) VALUES (1, 'parfum'), (1, 'fragrance'),
                                                                      (1, 'methylparaben'), (1, 'polen');
                INSERT INTO perfil_afeccion (perfil_id, nombre) VALUES (1, 'rosácea'), (1, 'me pica');
                """);
        flyway(null).migrate();
    }

    @Test
    void cadaEnumDelDominioExisteEnSuCatalogoDeBd() throws SQLException {
        assertEquals(nombres(Alergeno.values()), consulta("SELECT codigo FROM alergeno"));
        Set<String> condiciones = nombres(Afeccion.values());
        condiciones.addAll(nombres(TipoPiel.values()));
        assertEquals(condiciones, consulta("SELECT codigo FROM condicion"));
    }

    @Test
    void ningunAlergenoQuedaSinIngredientesInci() throws SQLException {
        Set<String> conMapeo = consulta("SELECT DISTINCT alergeno FROM alergeno_ingrediente");
        Set<String> sinMapeo = nombres(Alergeno.values());
        sinMapeo.removeAll(conMapeo);
        assertTrue(sinMapeo.isEmpty(), "Alérgenos que el motor nunca podría detectar: " + sinMapeo);
    }

    @Test
    void textoLibreConocidoSeMapeaYElDesconocidoSeRespalda() throws SQLException {
        assertEquals(Set.of("FRAGANCIA", "PARABENOS"), consulta("SELECT nombre FROM perfil_alergia"));
        assertEquals(Set.of("polen"), consulta("SELECT nombre FROM perfil_alergia_legacy"));
        assertEquals(Set.of("ROSACEA"), consulta("SELECT nombre FROM perfil_afeccion"));
        assertEquals(Set.of("me pica"), consulta("SELECT nombre FROM perfil_afeccion_legacy"));
        assertEquals(Set.of("<null>"), consulta("SELECT COALESCE(tipo_cabello, '<null>') FROM perfil_dermatologico"));
    }

    @Test
    void lasReglasDeIrritantesUsanCodigosDeCondicion() throws SQLException {
        Set<String> invalidas = consulta("SELECT condicion FROM regla_perfil WHERE condicion NOT IN (SELECT codigo FROM condicion)");
        assertTrue(invalidas.isEmpty(), "Reglas con condición inválida: " + invalidas);
        assertFalse(consulta("SELECT condicion FROM regla_perfil").isEmpty());
    }

    @Test
    void elCatalogoDeIngredientesEstaCompletoYDescrito() throws SQLException {
        assertTrue(consulta("SELECT nombre_inci FROM ingrediente").size() > 400);
        Set<String> sinDescripcion = consulta("SELECT nombre_inci FROM ingrediente WHERE descripcion IS NULL");
        assertTrue(sinDescripcion.isEmpty(), "Ingredientes sin nombre/función: " + sinDescripcion);
    }

    @Test
    void laBaseDeConocimientoTieneReglasBeneficiosEIncompatibilidades() throws SQLException {
        assertTrue(consulta("SELECT id::text FROM regla_perfil").size() > 100);
        assertTrue(consulta("SELECT id::text FROM beneficio").size() > 100);
        // Los 14 pares sembrados en V7 deben haber resuelto sus dos ingredientes.
        assertEquals(14, consulta("SELECT id::text FROM regla_incompatibilidad").size());
        // Cada condición del dominio tiene al menos una regla o un beneficio asociado (salvo NORMAL).
        Set<String> sinConocimiento = consulta("""
                SELECT codigo FROM condicion c WHERE codigo <> 'NORMAL'
                  AND NOT EXISTS (SELECT 1 FROM regla_perfil r WHERE r.condicion = c.codigo)
                  AND NOT EXISTS (SELECT 1 FROM beneficio b WHERE b.condicion = c.codigo)""");
        assertTrue(sinConocimiento.isEmpty(), "Condiciones sin conocimiento: " + sinConocimiento);
    }

    @Test
    void laEmbarazadaRecibeAlertaAltaPorRetinol() throws SQLException {
        assertEquals(Set.of("ALTA"), consulta(
                "SELECT severidad FROM regla_perfil WHERE ingrediente_inci = 'retinol' AND condicion = 'EMBARAZO_LACTANCIA'"));
    }

    @Test
    void losProductosDemoUsanTodoElCatalogoYTienenImagen() throws SQLException {
        assertEquals(78, consulta("SELECT id::text FROM producto WHERE vendedor_id = 2").size());
        Set<String> sinImagen = consulta("""
                SELECT nombre FROM producto p WHERE vendedor_id = 2
                  AND NOT EXISTS (SELECT 1 FROM producto_imagen i WHERE i.producto_id = p.id AND i.url LIKE 'https://%')""");
        assertTrue(sinImagen.isEmpty(), "Productos sin imagen: " + sinImagen);
        Set<String> sinUso = consulta("""
                SELECT nombre_inci FROM ingrediente i
                WHERE NOT EXISTS (SELECT 1 FROM producto_ingrediente pi WHERE pi.ingrediente_id = i.id)""");
        assertTrue(sinUso.isEmpty(), "Ingredientes del catálogo sin ningún producto: " + sinUso);
    }

    @Test
    void sinElVendedorDemoLaMigracionNoFallaNiSiembra() throws SQLException {
        ejecutar("CREATE DATABASE sin_vendedor");
        String url = PG.getJdbcUrl().replace("/" + PG.getDatabaseName(), "/sin_vendedor");
        Flyway.configure().dataSource(url, PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration").load().migrate();
        try (Connection c = DriverManager.getConnection(url, PG.getUsername(), PG.getPassword());
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM producto")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    // ---- utilidades ----

    private static Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }

    private static Connection conexion() throws SQLException {
        return DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
    }

    private static void ejecutar(String sql) throws SQLException {
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static Set<String> consulta(String sql) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection c = conexion(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) filas.add(rs.getString(1));
        }
        return new HashSet<>(filas);
    }

    private static Set<String> nombres(Enum<?>[] valores) {
        Set<String> out = new HashSet<>();
        for (Enum<?> v : valores) out.add(v.name());
        return out;
    }
}
