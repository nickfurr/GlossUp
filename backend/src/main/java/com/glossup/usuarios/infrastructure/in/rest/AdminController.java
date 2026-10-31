package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ModerarProductoUseCase;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.in.rest.dto.ProductoResponse;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.in.AdministrarUsuariosUseCase;
import com.glossup.usuarios.domain.port.in.RecuperarClaveUseCase;
import com.glossup.usuarios.infrastructure.out.persistence.entity.RolEntity;
import com.glossup.usuarios.infrastructure.out.persistence.entity.UsuarioEntity;
import com.glossup.usuarios.infrastructure.out.persistence.repository.RolJpaRepository;
import com.glossup.usuarios.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Usuarios, vendedores y moderación del catálogo")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UsuarioJpaRepository usuarioRepository;
    private final RolJpaRepository rolRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdministrarUsuariosUseCase administrar;
    private final RecuperarClaveUseCase recuperar;
    private final ModerarProductoUseCase moderar;

    public AdminController(UsuarioJpaRepository usuarioRepository,
                           RolJpaRepository rolRepository,
                           ProductoRepositoryPort productoRepository,
                           PasswordEncoder passwordEncoder,
                           AdministrarUsuariosUseCase administrar,
                           RecuperarClaveUseCase recuperar,
                           ModerarProductoUseCase moderar) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
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
            String motivoSuspension
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
            @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, message = "Mínimo 8 caracteres") String password
    ) {}

    @GetMapping("/usuarios")
    @Operation(summary = "Listar los usuarios registrados en el sistema")
    public ResponseEntity<List<UsuarioAdminResponse>> listarUsuarios() {
        List<UsuarioAdminResponse> lista = usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(UsuarioEntity::getId))
                .map(this::aResponse)
                .toList();
        return ResponseEntity.ok(lista);
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
        String emailNorm = request.email().trim().toLowerCase();
        if (usuarioRepository.findByEmailIgnoreCase(emailNorm).isPresent()) {
            throw new ReglaNegocioException("El correo '" + emailNorm + "' ya está registrado");
        }

        RolEntity rolVendedor = rolRepository.findByNombreIgnoreCase("VENDEDOR")
                .orElseGet(() -> rolRepository.save(new RolEntity(null, "VENDEDOR")));

        UsuarioEntity nuevo = new UsuarioEntity(
                null,
                emailNorm,
                passwordEncoder.encode(request.password()),
                request.nombre().trim(),
                true,
                OffsetDateTime.now(),
                Set.of(rolVendedor)
        );
        UsuarioEntity guardado = usuarioRepository.save(nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(aResponse(guardado));
    }

    @GetMapping("/productos")
    @Operation(summary = "Listar todos los productos registrados en el catálogo")
    public ResponseEntity<List<ProductoResponse>> listarProductos() {
        List<ProductoResponse> productos = productoRepository.listarTodos().stream()
                .sorted(Comparator.comparing(Producto::getId))
                .map(ProductoResponse::conModeracion)
                .toList();
        return ResponseEntity.ok(productos);
    }

    private UsuarioAdminResponse aResponse(UsuarioEntity u) {
        Set<String> roles = u.getRoles().stream()
                .map(RolEntity::getNombre)
                .collect(Collectors.toSet());
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), roles, u.getCreadoEn(), u.getMotivoSuspension());
    }

    private UsuarioAdminResponse aResponse(Usuario u) {
        Set<String> roles = u.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), roles, u.getCreadoEn(), u.getMotivoSuspension());
    }

    private static Long adminId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
