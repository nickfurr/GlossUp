package com.glossup.catalogo.infrastructure.in.rest;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ExtraerIngredientesImagenUseCase;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.in.RegistrarProductoUseCase;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.in.rest.dto.IngredientesImagenResponse;
import com.glossup.catalogo.domain.port.in.ActualizarProductoCommand;
import com.glossup.catalogo.domain.port.in.GestionarProductoUseCase;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.in.RegistrarProductoUseCase;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.in.rest.dto.ActualizarProductoRequest;
import com.glossup.catalogo.infrastructure.in.rest.dto.ProductoResponse;
import com.glossup.catalogo.infrastructure.in.rest.dto.RegistrarProductoRequest;
import com.glossup.shared.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo/productos")
@Tag(name = "Catálogo", description = "Gestión de catálogo de productos cosméticos e ingredientes INCI")
public class ProductoController {

    private final RegistrarProductoUseCase registrarProductoUseCase;
    private final GestionarProductoUseCase gestionarProductoUseCase;
    private final ProductoRepositoryPort productoRepository;
    private final ExtraerIngredientesImagenUseCase extraerIngredientesImagenUseCase;

    public ProductoController(RegistrarProductoUseCase registrarProductoUseCase,
                              GestionarProductoUseCase gestionarProductoUseCase,
                              ProductoRepositoryPort productoRepository,
                              ExtraerIngredientesImagenUseCase extraerIngredientesImagenUseCase) {
        this.registrarProductoUseCase = registrarProductoUseCase;
        this.gestionarProductoUseCase = gestionarProductoUseCase;
        this.productoRepository = productoRepository;
        this.extraerIngredientesImagenUseCase = extraerIngredientesImagenUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDEDOR')")
    @Operation(summary = "Registrar un nuevo producto en el catálogo (HU3)",
               description = "Registra un producto con sus hipervínculos de imágenes y ficha INCI completa obligatoria. Requiere rol VENDEDOR.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductoResponse> registrar(
            @Valid @RequestBody RegistrarProductoRequest request,
            Authentication authentication) {

        Long vendedorId = extraerVendedorId(authentication);

        List<RegistrarProductoCommand.IngredienteComando> ingredientesCmd = request.ingredientes().stream()
                .map(i -> new RegistrarProductoCommand.IngredienteComando(i.inci(), i.concentracion()))
                .toList();

        RegistrarProductoCommand command = new RegistrarProductoCommand(
                vendedorId,
                request.nombre(),
                request.marca(),
                request.categoria(),
                request.precio(),
                request.stock(),
                request.ph(),
                request.registroInvima(),
                request.imagenes(),
                ingredientesCmd
        );

        Producto producto = registrarProductoUseCase.registrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductoResponse.fromDomain(producto));
    }

    @PostMapping(value = "/extraer-ingredientes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDEDOR')")
    @Operation(summary = "Extraer ingredientes INCI desde foto de etiqueta (HU5)",
               description = "Gemini Vision analiza la imagen y devuelve la lista de ingredientes INCI. Requiere rol VENDEDOR.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IngredientesImagenResponse> extraerIngredientes(
            @RequestParam("imagen") MultipartFile imagen) throws IOException {

        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String mimeType = imagen.getContentType() != null ? imagen.getContentType() : "image/jpeg";
        List<String> ingredientes = extraerIngredientesImagenUseCase.extraer(imagen.getBytes(), mimeType);
        return ResponseEntity.ok(new IngredientesImagenResponse(ingredientes));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VENDEDOR')")
    @Operation(summary = "Modificar un producto existente del catálogo (HU4)",
               description = "Actualiza los datos generales, stock, imágenes y ficha INCI de un producto perteneciente al vendedor autenticado.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProductoRequest request,
            Authentication authentication) {

        Long vendedorId = extraerVendedorId(authentication);

        List<RegistrarProductoCommand.IngredienteComando> ingredientesCmd = request.ingredientes().stream()
                .map(i -> new RegistrarProductoCommand.IngredienteComando(i.inci(), i.concentracion()))
                .toList();

        ActualizarProductoCommand command = new ActualizarProductoCommand(
                id,
                vendedorId,
                request.nombre(),
                request.marca(),
                request.categoria(),
                request.precio(),
                request.stock(),
                request.ph(),
                request.registroInvima(),
                request.imagenes(),
                ingredientesCmd
        );

        Producto actualizado = gestionarProductoUseCase.actualizar(command);
        return ResponseEntity.ok(ProductoResponse.fromDomain(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VENDEDOR')")
    @Operation(summary = "Eliminar lógicamente un producto del catálogo (HU4)",
               description = "Despublica (activo = false) un producto perteneciente al vendedor autenticado, preservando la integridad de pedidos históricos.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            Authentication authentication) {

        Long vendedorId = extraerVendedorId(authentication);
        gestionarProductoUseCase.eliminar(id, vendedorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar productos por palabra clave (HU12)",
               description = "Busqueda basica de productos publicados cuyo nombre, marca o categoria contengan la palabra clave.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ProductoResponse>> buscar(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<ProductoResponse> resultados = productoRepository.buscarPorPalabraClave(q.trim()).stream()
                .map(ProductoResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar producto por ID")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        return productoRepository.buscarPorId(id)
                .map(ProductoResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + id));
    }

    @GetMapping("/mis-productos")
    @PreAuthorize("hasRole('VENDEDOR')")
    @Operation(summary = "Listar productos del vendedor autenticado",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ProductoResponse>> listarMisProductos(Authentication authentication) {
        Long vendedorId = extraerVendedorId(authentication);
        List<ProductoResponse> productos = productoRepository.buscarPorVendedor(vendedorId).stream()
                .map(ProductoResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(productos);
    }

    private Long extraerVendedorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return 1L;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return (long) Math.abs(authentication.getName().hashCode());
        }
    }
}
