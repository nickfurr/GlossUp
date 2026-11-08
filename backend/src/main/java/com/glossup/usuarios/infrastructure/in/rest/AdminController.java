package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.catalogo.domain.port.in.ModerarProductoUseCase;
import com.glossup.catalogo.infrastructure.in.rest.dto.ProductoResponse;
import com.glossup.usuarios.domain.model.Clave;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AdministrarUsuariosUseCase;
import com.glossup.usuarios.domain.port.in.RecuperarClaveUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Adaptador de entrada del administrador: solo traduce HTTP a casos de uso (auditoría OBS-T02). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Usuarios, vendedores y moderación del catálogo")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdministrarUsuariosUseCase administrar;
    private final RecuperarClaveUseCase recuperar;
    private final ModerarProductoUseCase moderar;

    public AdminController(AdministrarUsuariosUseCase administrar,
                           RecuperarClaveUseCase recuperar,
                           ModerarProductoUseCase moderar) {
        this.administrar = administrar;
        this.recuperar = recuperar;
        this.moderar = moderar;
    }

    public record UsuarioAdminResponse(
            Long id,
            String nombre,
            String email,
            boolean activo,
            Set<String> roles,
            OffsetDateTime creadoEn,
            String motivoSuspension,
            boolean pendienteAprobacion
    ) {}

    public record DatosUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150, message = "El nombre no puede superar 150 caracteres") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 255) String email
    ) {}

    public record RolRequest(@NotBlank(message = "Elige un rol") String rol) {}

    public record MotivoRequest(
            @NotBlank(message = "Escribe el motivo") @Size(max = 300, message = "El motivo no puede superar 300 caracteres") String motivo
    ) {}

    public record EnlaceResponse(String enlace) {}

    public record CrearVendedorRequest(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = Clave.LONGITUD_MINIMA, max = Clave.LONGITUD_MAXIMA, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = Clave.REGEX_COMPLEJIDAD, message = Clave.MENSAJE_REQUISITOS)
            String password
    ) {}

    @GetMapping("/usuarios")
    @Operation(summary = "Listar los usuarios registrados en el sistema")
    public List<UsuarioAdminResponse> listarUsuarios() {
        return administrar.listar().stream().map(this::aResponse).toList();
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Corregir el nombre y el correo de una cuenta")
    public UsuarioAdminResponse actualizarDatos(@PathVariable Long id, @Valid @RequestBody DatosUsuarioRequest datos) {
        return aResponse(administrar.actualizarDatos(id, datos.nombre(), datos.email()));
    }

    @PutMapping("/usuarios/{id}/rol")
    @Operation(summary = "Cambiar el rol de una cuenta", description = "Cierra sus sesiones abiertas. No se aplica a la cuenta propia.")
    public UsuarioAdminResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody RolRequest datos, Authentication auth) {
        return aResponse(administrar.cambiarRol(adminId(auth), id, datos.rol()));
    }

    @PostMapping("/usuarios/{id}/suspender")
    @Operation(summary = "Suspender una cuenta", description = "No puede entrar, sus sesiones dejan de valer y, si es vendedor, sus productos salen del catálogo.")
    public UsuarioAdminResponse suspender(@PathVariable Long id, @Valid @RequestBody MotivoRequest datos, Authentication auth) {
        return aResponse(administrar.suspender(adminId(auth), id, datos.motivo()));
    }

    @PostMapping("/usuarios/{id}/reactivar")
    @Operation(summary = "Reactivar una cuenta suspendida y los productos bloqueados por esa suspensión")
    public UsuarioAdminResponse reactivar(@PathVariable Long id) {
        return aResponse(administrar.reactivar(id));
    }

    @PostMapping("/usuarios/{id}/cerrar-sesiones")
    @Operation(summary = "Cerrar todas las sesiones abiertas de una cuenta", description = "Tendrá que volver a iniciar sesión en cada dispositivo.")
    public UsuarioAdminResponse cerrarSesiones(@PathVariable Long id) {
        return aResponse(administrar.cerrarSesiones(id));
    }

    @PostMapping("/usuarios/{id}/aprobar")
    @Operation(summary = "Aprobar una cuenta de vendedor que se registró sola", description = "Hasta entonces no puede iniciar sesión.")
    public UsuarioAdminResponse aprobarVendedor(@PathVariable Long id, Authentication auth) {
        return aResponse(administrar.aprobarVendedor(adminId(auth), id));
    }

    @PostMapping("/usuarios/{id}/recuperacion")
    @Operation(summary = "Generar un enlace para que el usuario cree una contraseña nueva",
               description = "Un solo uso, 24 horas. El administrador nunca ve ni fija la contraseña.")
    public EnlaceResponse generarEnlace(@PathVariable Long id) {
        return new EnlaceResponse(recuperar.generarEnlace(id));
    }

    @PostMapping("/productos/{id}/banear")
    @Operation(summary = "Bloquear un producto", description = "Sale del catálogo y el vendedor no puede volver a publicarlo.")
    public ProductoResponse banear(@PathVariable Long id, @Valid @RequestBody MotivoRequest datos) {
        return ProductoResponse.conModeracion(moderar.banear(id, datos.motivo()));
    }

    @PostMapping("/productos/{id}/desbanear")
    @Operation(summary = "Quitar el bloqueo de un producto")
    public ProductoResponse desbanear(@PathVariable Long id) {
        return ProductoResponse.conModeracion(moderar.desbanear(id));
    }

    @PostMapping("/vendedores")
    @Operation(summary = "Registrar una nueva cuenta con rol VENDEDOR")
    public ResponseEntity<UsuarioAdminResponse> crearVendedor(@Valid @RequestBody CrearVendedorRequest request) {
        Usuario nuevo = administrar.crearVendedor(request.nombre().trim(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(nuevo));
    }

    @GetMapping("/productos")
    @Operation(summary = "Listar todos los productos registrados en el catálogo")
    public List<ProductoResponse> listarProductos() {
        return moderar.listarTodos().stream().map(ProductoResponse::conModeracion).toList();
    }

    private UsuarioAdminResponse aResponse(Usuario u) {
        Set<String> roles = u.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), roles, u.getCreadoEn(),
                u.getMotivoSuspension(), u.isPendienteAprobacion());
    }

    private static Long adminId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
