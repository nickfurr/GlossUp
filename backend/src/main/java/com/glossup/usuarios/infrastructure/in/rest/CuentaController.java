package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.usuarios.domain.port.in.GestionarCuentaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cuenta del usuario autenticado. El usuario siempre sale del token. */
@RestController
@RequestMapping("/api/cuenta")
@Tag(name = "Cuenta", description = "Datos de la cuenta propia")
@SecurityRequirement(name = "bearerAuth")
public class CuentaController {

    public record NombreRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar 150 caracteres") String nombre) {}

    public record ClaveRequest(
            @NotBlank(message = "Escribe tu contraseña actual") String actual,
            @NotBlank(message = "Escribe la contraseña nueva")
            @Size(min = 8, max = 120, message = "La contraseña debe tener entre 8 y 120 caracteres") String nueva) {}

    public record EliminarCuentaRequest(@NotBlank(message = "Escribe tu contraseña para confirmar") String clave) {}

    private final GestionarCuentaUseCase cuenta;

    public CuentaController(GestionarCuentaUseCase cuenta) {
        this.cuenta = cuenta;
    }

    @PutMapping("/nombre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi nombre")
    public void cambiarNombre(@Valid @RequestBody NombreRequest datos, Authentication auth) {
        cuenta.cambiarNombre(Long.parseLong(auth.getName()), datos.nombre());
    }

    @PutMapping("/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi contraseña", description = "Exige la contraseña actual; si no coincide responde 422.")
    public void cambiarClave(@Valid @RequestBody ClaveRequest datos, Authentication auth) {
        cuenta.cambiarClave(Long.parseLong(auth.getName()), datos.actual(), datos.nueva());
    }

    /** POST y no DELETE: algunos proxies descartan el cuerpo de un DELETE, y aquí lleva la contraseña. */
    @PostMapping("/eliminar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar mi cuenta",
               description = "Borra el perfil dermatológico y las direcciones, y deja la cuenta anónima e inactiva. "
                       + "Los pedidos se conservan por obligación legal. Exige la contraseña actual.")
    public void eliminarCuenta(@Valid @RequestBody EliminarCuentaRequest datos, Authentication auth) {
        cuenta.eliminarCuenta(Long.parseLong(auth.getName()), datos.clave());
    }
}
