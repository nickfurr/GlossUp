package com.glossup.recomendacion.infrastructure.out.catalogo;

import com.glossup.catalogo.infrastructure.out.persistence.adapter.ProductoPersistenceAdapter;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compras simultáneas del mismo producto contra un PostgreSQL real (auditoría R-05 / NC-T06): antes, 13 de 15
 * respondían 500. Ahora cada compra o reserva su unidad o recibe "stock insuficiente", y el stock nunca baja de 0.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductoPersistenceAdapter.class, InventarioCatalogoAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED) // cada compra en su propia transacción, como en producción
class InventarioConcurrenteTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired InventarioCatalogoAdapter inventario;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacciones;

    @Test
    void quinceComprasSimultaneasDeCincoUnidades_cincoReservanYDiezReciben422() throws Exception {
        Long id = jdbc.queryForObject(
                "INSERT INTO producto (nombre, precio, stock) VALUES ('Sérum de prueba', 50000, 5) RETURNING id", Long.class);
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(15);
        List<Future<String>> compras = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Callable<String> compra = () -> {
                salida.await();
                try {
                    tx.executeWithoutResult(s -> inventario.reservar(id, 1));
                    return "reservada";
                } catch (ReglaNegocioException e) {
                    return "sin stock";
                }
            };
            compras.add(hilos.submit(compra));
        }
        salida.countDown();
        List<String> resultados = new ArrayList<>();
        for (Future<String> f : compras) {
            resultados.add(f.get()); // cualquier otra excepción (la que antes era un 500) hace fallar la prueba
        }
        hilos.shutdown();

        assertEquals(5, resultados.stream().filter("reservada"::equals).count());
        assertEquals(10, resultados.stream().filter("sin stock"::equals).count());
        assertEquals(0, jdbc.queryForObject("SELECT stock FROM producto WHERE id = ?", Integer.class, id));
    }

    @Test
    void liberarDevuelveLasUnidadesAlStock() {
        Long id = jdbc.queryForObject(
                "INSERT INTO producto (nombre, precio, stock) VALUES ('Tónico de prueba', 30000, 2) RETURNING id", Long.class);
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        tx.executeWithoutResult(s -> inventario.reservar(id, 2));
        tx.executeWithoutResult(s -> inventario.liberar(id, 2));
        assertEquals(2, jdbc.queryForObject("SELECT stock FROM producto WHERE id = ?", Integer.class, id));
    }
}
