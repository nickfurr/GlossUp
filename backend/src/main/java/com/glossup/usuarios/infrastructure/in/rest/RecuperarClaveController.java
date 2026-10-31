package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.usuarios.domain.port.in.RecuperarClaveUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Público: quien olvidó la contraseña no tiene sesión. Lo protege el token de un solo uso. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Recuperación de contraseña")
public class RecuperarClaveController {

    public record SolicitudRequest(@NotBlank @Email String email) {}

    public record RestablecerRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 120, message = "La contraseña debe tener entre 8 y 120 caracteres") String nueva) {}

    private final RecuperarClaveUseCase recuperar;

    public RecuperarClaveController(RecuperarClaveUseCase recuperar) {
        this.recuperar = recuperar;
    }

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pedir el enlace de recuperación", description = "Responde igual exista o no el correo.")
    public void solicitar(@Valid @RequestBody SolicitudRequest datos) {
        recuperar.solicitar(datos.email());
    }

    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Crear la contraseña nueva con el enlace")
    public void restablecer(@Valid @RequestBody RestablecerRequest datos) {
        recuperar.restablecer(datos.token(), datos.nueva());
    }
}
