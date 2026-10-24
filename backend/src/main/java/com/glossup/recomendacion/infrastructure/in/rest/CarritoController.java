package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.Carrito;
import com.glossup.recomendacion.domain.port.in.GestionarCarritoUseCase;
import com.glossup.recomendacion.infrastructure.in.rest.dto.CantidadRequest;
import com.glossup.recomendacion.infrastructure.in.rest.dto.CarritoResponse;
import com.glossup.recomendacion.infrastructure.in.rest.dto.LineaCarritoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API REST del carrito de compras del cliente (HU9 - CRUD de carrito). El usuario se toma del
 * token JWT. Agregar/cambiar/quitar no reservan inventario; la reserva ocurre en el checkout.
 */
@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Carrito", description = "Gestión del carrito de compras del cliente (HU9)")
@SecurityRequirement(name = "bearerAuth")
public class CarritoController {

    private final GestionarCarritoUseCase gestionarCarritoUseCase;

    public CarritoController(GestionarCarritoUseCase gestionarCarritoUseCase) {
        this.gestionarCarritoUseCase = gestionarCarritoUseCase;
    }

    @GetMapping
    @Operation(summary = "Ver mi carrito")
    public ResponseEntity<CarritoResponse> ver(Authentication authentication) {
        Carrito carrito = gestionarCarritoUseCase.verCarrito(extraerUsuarioId(authentication));
        return ResponseEntity.ok(CarritoResponse.fromDomain(carrito));
    }

    @PostMapping("/items")
    @Operation(summary = "Agregar un producto al carrito (acumula si ya está)")
    public ResponseEntity<CarritoResponse> agregar(
            @Valid @RequestBody LineaCarritoRequest request,
            Authentication authentication) {
        Carrito carrito = gestionarCarritoUseCase.agregarItem(
                extraerUsuarioId(authentication), request.productoId(), request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(CarritoResponse.fromDomain(carrito));
    }

    @PutMapping("/items/{productoId}")
    @Operation(summary = "Cambiar la cantidad de un producto del carrito")
    public ResponseEntity<CarritoResponse> cambiarCantidad(
            @PathVariable Long productoId,
            @Valid @RequestBody CantidadRequest request,
            Authentication authentication) {
        Carrito carrito = gestionarCarritoUseCase.cambiarCantidad(
                extraerUsuarioId(authentication), productoId, request.cantidad());
        return ResponseEntity.ok(CarritoResponse.fromDomain(carrito));
    }

    @DeleteMapping("/items/{productoId}")
    @Operation(summary = "Quitar un producto del carrito")
    public ResponseEntity<CarritoResponse> quitar(
            @PathVariable Long productoId,
            Authentication authentication) {
        Carrito carrito = gestionarCarritoUseCase.quitarItem(extraerUsuarioId(authentication), productoId);
        return ResponseEntity.ok(CarritoResponse.fromDomain(carrito));
    }

    @DeleteMapping
    @Operation(summary = "Vaciar el carrito")
    public ResponseEntity<CarritoResponse> vaciar(Authentication authentication) {
        Carrito carrito = gestionarCarritoUseCase.vaciar(extraerUsuarioId(authentication));
        return ResponseEntity.ok(CarritoResponse.fromDomain(carrito));
    }

    private Long extraerUsuarioId(Authentication authentication) {
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
