package com.glossup.usuarios.domain.model;

import com.glossup.shared.domain.ReglaNegocioException;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Entidad de dominio Usuario (módulo de identidad).
 *
 * <p>Invariantes:</p>
 * <ul>
 *   <li>Email y nombre válidos.</li>
 *   <li>El hash de la contraseña nunca va vacío (la contraseña en claro no vive en el dominio).</li>
 *   <li>Todo usuario tiene al menos un rol.</li>
 * </ul>
 */
public class Usuario {

    private final Long id;            // null hasta que se persiste
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String email;
    private String motivoSuspension;
    private OffsetDateTime sesionesDesde;
    private OffsetDateTime politicaAceptadaEn;
    private String politicaVersion;
    private String nombre;
    private String passwordHash;
    private boolean activo;
    private boolean pendienteAprobacion;
    private final Set<Rol> roles;
    private final OffsetDateTime creadoEn;

    /** Constructor de reconstrucción (desde persistencia): todos los campos conocidos. */
    public Usuario(Long id, String email, String nombre, String passwordHash,
                   boolean activo, Set<Rol> roles, OffsetDateTime creadoEn) {
        this.id = id;
        this.email = validarEmail(email);
        this.nombre = validarNombre(nombre);
        this.passwordHash = validarHash(passwordHash);
        this.activo = activo;
        this.roles = (roles == null || roles.isEmpty())
                ? EnumSet.of(Rol.CLIENTE)
                : EnumSet.copyOf(roles);
        this.creadoEn = creadoEn != null ? creadoEn : OffsetDateTime.now();
    }

    /** Fábrica para registrar un usuario nuevo con su rol inicial. */
    public static Usuario registrar(String email, String hash, String nombre, Rol rol) {
        return new Usuario(null, email, nombre, hash, true,
                EnumSet.of(Objects.requireNonNull(rol, "rol requerido")), OffsetDateTime.now());
    }

    // ---- Reglas de negocio / comportamiento ----

    public void asignarRol(Rol rol) {
        roles.add(Objects.requireNonNull(rol, "rol requerido"));
    }

    public void quitarRol(Rol rol) {
        if (roles.size() == 1 && roles.contains(rol)) {
            throw new ReglaNegocioException("El usuario debe conservar al menos un rol");
        }
        roles.remove(rol);
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    /** Suspensión por el administrador: no puede entrar y sus sesiones abiertas dejan de valer. */
    public void suspender(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Escribe el motivo de la suspensión");
        }
        this.activo = false;
        this.motivoSuspension = motivo.trim();
    }

    public void reactivar() {
        this.activo = true;
        this.motivoSuspension = null;
    }

    /** Vendedor que se registró solo: no entra hasta que un administrador lo apruebe (auditoría OBS-T08). */
    public void quedarPendienteDeAprobacion() {
        if (!tieneRol(Rol.VENDEDOR)) {
            throw new ReglaNegocioException("Solo las cuentas de vendedor esperan aprobación");
        }
        this.pendienteAprobacion = true;
    }

    public void aprobar() {
        if (!pendienteAprobacion) {
            throw new ReglaNegocioException("La cuenta no tiene una aprobación pendiente");
        }
        this.pendienteAprobacion = false;
    }

    /** Reconstrucción desde persistencia. */
    public Usuario conAprobacionPendiente(boolean pendiente) {
        this.pendienteAprobacion = pendiente;
        return this;
    }

    public void cerrarSesiones(OffsetDateTime desde) {
        this.sesionesDesde = desde;
    }

    /** Reconstrucción desde persistencia. */
    public Usuario conModeracion(String motivoSuspension, OffsetDateTime sesionesDesde) {
        this.motivoSuspension = motivoSuspension;
        this.sesionesDesde = sesionesDesde;
        return this;
    }

    /** Prueba de la autorización del titular: cuándo aceptó y qué versión de la política (Ley 1581 art. 9). */
    public void aceptarPolitica(String version, OffsetDateTime cuando) {
        this.politicaVersion = Objects.requireNonNull(version, "versión requerida");
        this.politicaAceptadaEn = Objects.requireNonNull(cuando, "fecha requerida");
    }

    /** Reconstrucción desde persistencia. */
    public Usuario conPolitica(OffsetDateTime aceptadaEn, String version) {
        this.politicaAceptadaEn = aceptadaEn;
        this.politicaVersion = version;
        return this;
    }

    /**
     * Supresión pedida por el titular (Ley 1581 art. 8 e): se borran sus datos de contacto y no puede volver a
     * entrar. La fila se conserva anónima porque sus pedidos y comprobantes deben guardarse por ley.
     */
    public void suprimir(String hashInutilizable, OffsetDateTime cuando) {
        this.email = "eliminado-" + Objects.requireNonNull(id, "usuario sin persistir") + "@glossup.invalid";
        this.nombre = "Cuenta eliminada";
        this.passwordHash = validarHash(hashInutilizable);
        this.activo = false;
        this.motivoSuspension = "Cuenta eliminada por su titular";
        this.sesionesDesde = cuando;
    }

    public void cambiarEmail(String nuevoEmail) {
        this.email = validarEmail(nuevoEmail);
    }

    /** Deja al usuario con un solo rol. */
    public void cambiarRol(Rol rol) {
        this.roles.clear();
        this.roles.add(Objects.requireNonNull(rol, "rol requerido"));
        this.pendienteAprobacion = false; // el rol lo asigna un administrador: ya no hay nada que aprobar
    }

    public void cambiarNombre(String nuevoNombre) {
        this.nombre = validarNombre(nuevoNombre);
    }

    public void cambiarPasswordHash(String nuevoHash) {
        this.passwordHash = validarHash(nuevoHash);
    }

    // ---- Validaciones de invariantes ----

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre no puede estar vacío");
        }
        return nombre.trim();
    }

    private static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ReglaNegocioException("El correo no puede estar vacío");
        }
        String normalizado = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizado).matches()) {
            throw new ReglaNegocioException("El correo no tiene un formato válido");
        }
        return normalizado;
    }

    private static String validarHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new ReglaNegocioException("El hash de la contraseña es obligatorio");
        }
        return hash;
    }

    // ---- Accesores ----

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public boolean isPendienteAprobacion() { return pendienteAprobacion; }
    public String getMotivoSuspension() { return motivoSuspension; }
    public OffsetDateTime getSesionesDesde() { return sesionesDesde; }
    public OffsetDateTime getPoliticaAceptadaEn() { return politicaAceptadaEn; }
    public String getPoliticaVersion() { return politicaVersion; }
    public Set<Rol> getRoles() { return Set.copyOf(roles); }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
