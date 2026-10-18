package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.in.rest.dto.ProductoResponse;
import com.glossup.shared.domain.ReglaNegocioException;
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
@Tag(name = "Administración", description = "Consulta de usuarios, creación de vendedores y listado de productos")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UsuarioJpaRepository usuarioRepository;
    private final RolJpaRepository rolRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UsuarioJpaRepository usuarioRepository,
                           RolJpaRepository rolRepository,
                           ProductoRepositoryPort productoRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record UsuarioAdminResponse(
            Long id,
            String nombre,
            String email,
            boolean activo,
            Set<String> roles,
            OffsetDateTime creadoEn
    ) {}

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
                .map(ProductoResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(productos);
    }

    private UsuarioAdminResponse aResponse(UsuarioEntity u) {
        Set<String> roles = u.getRoles().stream()
                .map(RolEntity::getNombre)
                .collect(Collectors.toSet());
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getEmail(), u.isActivo(), roles, u.getCreadoEn());
    }
}
