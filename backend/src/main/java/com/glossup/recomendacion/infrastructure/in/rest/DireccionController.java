package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.port.in.GestionarDireccionesUseCase;
import com.glossup.recomendacion.infrastructure.in.rest.dto.DireccionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Libreta de direcciones del cliente (HU14). El dueño siempre sale del token. */
@RestController
@RequestMapping("/api/direcciones")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Direcciones", description = "Libreta de direcciones de entrega (HU14)")
@SecurityRequirement(name = "bearerAuth")
public class DireccionController {

    private final GestionarDireccionesUseCase direcciones;

    public DireccionController(GestionarDireccionesUseCase direcciones) {
        this.direcciones = direcciones;
    }

    @GetMapping
    @Operation(summary = "Mis direcciones, la principal primero")
    public List<Direccion> listar(Authentication auth) {
        return direcciones.listar(usuarioId(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar una dirección")
    public Direccion crear(@Valid @RequestBody DireccionRequest datos, Authentication auth) {
        return direcciones.crear(datos.aDominio(usuarioId(auth)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una dirección propia")
    public Direccion actualizar(@PathVariable Long id, @Valid @RequestBody DireccionRequest datos, Authentication auth) {
        return direcciones.actualizar(id, datos.aDominio(usuarioId(auth)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar una dirección propia", description = "Los pedidos ya hechos conservan su copia de la dirección.")
    public void eliminar(@PathVariable Long id, Authentication auth) {
        direcciones.eliminar(usuarioId(auth), id);
    }

    private static Long usuarioId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
