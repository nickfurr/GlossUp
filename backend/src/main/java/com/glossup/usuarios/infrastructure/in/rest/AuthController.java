package com.glossup.usuarios.infrastructure.in.rest;

import com.glossup.usuarios.domain.port.in.AutenticarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.AutenticacionResultado;
import com.glossup.usuarios.domain.port.in.AutenticarUsuarioUseCase;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioCommand;
import com.glossup.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.glossup.usuarios.infrastructure.in.rest.dto.LoginRequest;
import com.glossup.usuarios.infrastructure.in.rest.dto.LoginResponse;
import com.glossup.usuarios.infrastructure.in.rest.dto.RegistrarUsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;

    public AuthController(AutenticarUsuarioUseCase autenticarUsuarioUseCase,
                          RegistrarUsuarioUseCase registrarUsuarioUseCase) {
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AutenticacionResultado resultado = autenticarUsuarioUseCase.autenticar(
                new AutenticarUsuarioCommand(request.email(), request.password()));
        Set<String> roles = resultado.usuario().getRoles().stream()
                .map(rol -> rol.name())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return ResponseEntity.ok(new LoginResponse(resultado.token(), resultado.usuario().getId(),
                resultado.usuario().getEmail(), resultado.usuario().getNombre(), roles));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrarUsuarioRequest request) {
        registrarUsuarioUseCase.registrar(new RegistrarUsuarioCommand(
                request.email(), request.nombre(), request.password(), request.rol()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
