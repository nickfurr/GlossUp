package com.glossup.perfil.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad de dominio: perfil dermatológico del usuario.
 *
 * <p>Alimenta al motor de compatibilidad (proceso "Gestión del perfil dermatológico").
 * Soporta la gestión dinámica de alergias y afecciones (HU2: agregar/quitar).</p>
 *
 * <p>Invariantes:</p>
 * <ul>
 *   <li>Siempre está asociado a un usuario ({@code usuarioId}).</li>
 *   <li>pH, tipo de piel y cabello son opcionales, pero si se dan deben ser válidos.</li>
 *   <li>Afecciones y alergias son catálogos cerrados (enums): no se admite texto libre.</li>
 * </ul>
 */
public class PerfilDermatologico {

    private final Long id;
    private final Long usuarioId;
    private Ph ph;
    private TipoPiel tipoPiel;
    private TipoCabello tipoCabello;
    private final Set<Afeccion> afecciones;
    private final Set<Alergeno> alergias;
    private OffsetDateTime actualizadoEn;

    public PerfilDermatologico(Long id, Long usuarioId, Ph ph, TipoPiel tipoPiel,
                               TipoCabello tipoCabello, Set<Afeccion> afecciones, Set<Alergeno> alergias,
                               OffsetDateTime actualizadoEn) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId requerido");
        this.ph = ph;
        this.tipoPiel = tipoPiel;
        this.tipoCabello = tipoCabello;
        this.afecciones = new LinkedHashSet<>();
        this.alergias = new LinkedHashSet<>();
        if (afecciones != null) afecciones.forEach(this::agregarAfeccion);
        if (alergias != null) alergias.forEach(this::agregarAlergia);
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : OffsetDateTime.now();
    }

    /** Fábrica para crear el perfil inicial de un usuario (HU1). */
    public static PerfilDermatologico crear(Long usuarioId, Ph ph, TipoPiel tipoPiel, TipoCabello tipoCabello) {
        return new PerfilDermatologico(null, usuarioId, ph, tipoPiel, tipoCabello,
                Set.of(), Set.of(), OffsetDateTime.now());
    }

    // ---- Reglas de negocio / comportamiento (HU2) ----

    public void agregarAlergia(Alergeno alergia) {
        alergias.add(requerido(alergia, "alergia"));
        tocar();
    }

    public void quitarAlergia(Alergeno alergia) {
        alergias.remove(alergia);
        tocar();
    }

    public void agregarAfeccion(Afeccion afeccion) {
        afecciones.add(requerido(afeccion, "afección"));
        tocar();
    }

    public void quitarAfeccion(Afeccion afeccion) {
        afecciones.remove(afeccion);
        tocar();
    }

    public void actualizarBiotipo(Ph ph, TipoPiel tipoPiel, TipoCabello tipoCabello) {
        this.ph = ph;
        this.tipoPiel = tipoPiel;
        this.tipoCabello = tipoCabello;
        tocar();
    }

    public boolean esAlergicoA(Alergeno alergeno) {
        return alergias.contains(alergeno);
    }

    public boolean tieneAfeccion(Afeccion afeccion) {
        return afecciones.contains(afeccion);
    }

    private void tocar() {
        this.actualizadoEn = OffsetDateTime.now();
    }

    private static <T> T requerido(T valor, String campo) {
        if (valor == null) throw new ReglaNegocioException("La " + campo + " es obligatoria");
        return valor;
    }

    // ---- Accesores ----

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Ph getPh() { return ph; }
    public TipoPiel getTipoPiel() { return tipoPiel; }
    public TipoCabello getTipoCabello() { return tipoCabello; }
    public Set<Afeccion> getAfecciones() { return Set.copyOf(afecciones); }
    public Set<Alergeno> getAlergias() { return Set.copyOf(alergias); }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}
