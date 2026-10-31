package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.port.out.DireccionRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DireccionPersistenceAdapter implements DireccionRepositoryPort {

    private static final String COLUMNAS =
            "usuario_id, nombre_recibe, telefono, departamento, ciudad, barrio, direccion, complemento, indicaciones, principal";
    private static final RowMapper<Direccion> FILA = (rs, n) -> new Direccion(
            rs.getLong("id"), rs.getLong("usuario_id"), rs.getString("nombre_recibe"), rs.getString("telefono"),
            rs.getString("departamento"), rs.getString("ciudad"), rs.getString("barrio"), rs.getString("direccion"),
            rs.getString("complemento"), rs.getString("indicaciones"), rs.getBoolean("principal"));

    private final JdbcTemplate jdbc;

    public DireccionPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Direccion> listar(Long usuarioId) {
        return jdbc.query("SELECT id, " + COLUMNAS + " FROM direccion WHERE usuario_id = ? ORDER BY principal DESC, id",
                FILA, usuarioId);
    }

    @Override
    public Optional<Direccion> buscar(Long id) {
        return jdbc.query("SELECT id, " + COLUMNAS + " FROM direccion WHERE id = ?", FILA, id).stream().findFirst();
    }

    @Override
    public Direccion guardar(Direccion d) {
        Object[] valores = {d.usuarioId(), d.nombreRecibe(), d.telefono(), d.departamento(), d.ciudad(), d.barrio(), d.direccion(),
                d.complemento(), d.indicaciones(), d.principal()};
        if (d.id() == null) {
            Long id = jdbc.queryForObject("INSERT INTO direccion (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class, valores);
            return d.conId(id, d.principal());
        }
        jdbc.update("UPDATE direccion SET (" + COLUMNAS + ") = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) WHERE id = " + d.id(), valores);
        return d;
    }

    @Override
    public void eliminar(Long id) {
        jdbc.update("DELETE FROM direccion WHERE id = ?", id);
    }

    @Override
    public void quitarPrincipal(Long usuarioId) {
        jdbc.update("UPDATE direccion SET principal = FALSE WHERE usuario_id = ? AND principal", usuarioId);
    }
}
