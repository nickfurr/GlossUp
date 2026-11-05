package com.glossup.catalogo.infrastructure.out.persistence;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.out.persistence.adapter.ProductoPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Las consultas paginadas del catálogo contra PostgreSQL real (auditoría R-14). */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductoPersistenceAdapter.class)
class CatalogoPaginadoTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired ProductoRepositoryPort productos;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void sembrar() {
        jdbc.update("""
                INSERT INTO producto (nombre, categoria, precio, stock, activo, baneado) VALUES
                  ('Sérum 1', 'Sérums', 1000, 5, true, false),
                  ('Crema 1', 'Cremas', 1000, 5, true, false),
                  ('Sérum 2', 'Sérums', 1000, 5, true, false),
                  ('Sérum agotado', 'Sérums', 1000, 0, true, false),
                  ('Sérum bloqueado', 'Sérums', 1000, 5, true, true),
                  ('Sérum de baja', 'Sérums', 1000, 5, false, false)""");
    }

    private static List<String> nombres(List<Producto> ps) {
        return ps.stream().map(Producto::getNombre).toList();
    }

    @Test
    void paginaSoloLoPublicadoYCuentaElTotal() {
        var primera = productos.catalogo(null, 0, 2);
        assertEquals(3, primera.total());
        assertEquals(List.of("Sérum 1", "Crema 1"), nombres(primera.productos()));
        assertEquals(List.of("Sérum 2"), nombres(productos.catalogo(null, 1, 2).productos()));
    }

    @Test
    void filtraPorCategoria() {
        var serums = productos.catalogo("Sérums", 0, 10);
        assertEquals(2, serums.total());
        assertEquals(List.of("Sérum 1", "Sérum 2"), nombres(serums.productos()));
    }

    @Test
    void elIndiceTraeIdYCategoriaDeLoPublicado() {
        assertEquals(List.of("Sérums", "Cremas", "Sérums"),
                productos.indiceCatalogo().stream().map(ProductoRepositoryPort.EntradaCatalogo::categoria).toList());
    }
}
