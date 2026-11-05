package com.glossup.perfil.infrastructure.in.rest;

import com.glossup.shared.security.UsuarioAutenticado;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.perfil.infrastructure.in.rest.dto.AfeccionRequest;
import com.glossup.perfil.infrastructure.in.rest.dto.AlergiaRequest;
import com.glossup.perfil.infrastructure.in.rest.dto.ActualizarPerfilRequest;
import com.glossup.perfil.infrastructure.in.rest.dto.OpcionesPerfilResponse;
import com.glossup.perfil.infrastructure.in.rest.dto.PerfilResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil/me")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Perfil Dermatológico", description = "Consulta y gestión dinámica de alergias y afecciones del cliente (HU2)")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    private final GestionarPerfilUseCase gestionarPerfilUseCase;

    public PerfilController(GestionarPerfilUseCase gestionarPerfilUseCase) {
        this.gestionarPerfilUseCase = gestionarPerfilUseCase;
    }

    @GetMapping
    @Operation(summary = "Obtener mi perfil dermatológico",
               description = "Retorna el perfil dermatológico del cliente autenticado. Si aún no existe, crea uno inicial vacío.")
    public ResponseEntity<PerfilResponse> obtenerMiPerfil(Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.obtenerPerfil(usuarioId);
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @PostMapping("/autorizacion")
    @Operation(summary = "Autorizar el uso de mis datos de salud",
               description = "Autorización explícita y facultativa (Ley 1581 art. 6). Sin ella no se pueden guardar "
                       + "biotipo, alergias ni afecciones.")
    public ResponseEntity<PerfilResponse> autorizarDatosSensibles(Authentication authentication) {
        return ResponseEntity.ok(PerfilResponse.fromDomain(
                gestionarPerfilUseCase.autorizarDatosSensibles(UsuarioAutenticado.id(authentication))));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revocar la autorización y borrar mi perfil",
               description = "Borra el biotipo, las alergias y las afecciones (Ley 1581 art. 8 e).")
    public void eliminarMiPerfil(Authentication authentication) {
        gestionarPerfilUseCase.eliminarPerfil(UsuarioAutenticado.id(authentication));
    }

    @GetMapping("/opciones")
    @Operation(summary = "Catálogos del perfil",
               description = "Tipos de piel y cabello, alergias y afecciones válidas para las listas desplegables.")
    public ResponseEntity<OpcionesPerfilResponse> opciones() {
        return ResponseEntity.ok(OpcionesPerfilResponse.crear());
    }

    @PutMapping
    @Operation(summary = "Actualizar mi perfil dermatológico")
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest request,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.actualizarBiotipo(
                usuarioId, request.ph(), request.tipoPiel(), request.tipoCabello());
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @PostMapping("/alergias")
    @Operation(summary = "Agregar una alergia (INCI) al perfil (HU2)",
               description = "Agrega dinámicamente un ingrediente cosmético (INCI) a la lista de alergias del cliente sin duplicados.")
    public ResponseEntity<PerfilResponse> agregarAlergia(
            @Valid @RequestBody AlergiaRequest request,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.agregarAlergia(usuarioId, request.alergia());
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @DeleteMapping("/alergias")
    @Operation(summary = "Quitar una alergia (INCI) del perfil mediante body (HU2)",
               description = "Elimina dinámicamente un ingrediente (INCI) de la lista de alergias del cliente.")
    public ResponseEntity<PerfilResponse> quitarAlergiaPorBody(
            @Valid @RequestBody AlergiaRequest request,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.quitarAlergia(usuarioId, request.alergia());
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @DeleteMapping("/alergias/{alergia}")
    @Operation(summary = "Quitar una alergia (INCI) del perfil por parámetro de ruta (HU2)",
               description = "Elimina dinámicamente un ingrediente (INCI) de la lista de alergias del cliente.")
    public ResponseEntity<PerfilResponse> quitarAlergiaPorRuta(
            @PathVariable String alergia,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.quitarAlergia(usuarioId, alergia);
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @PostMapping("/afecciones")
    @Operation(summary = "Agregar una afección o condición dermatológica al perfil (HU2)",
               description = "Agrega dinámicamente una condición dermatológica (ej. acné, rosácea, dermatitis) al perfil del cliente.")
    public ResponseEntity<PerfilResponse> agregarAfeccion(
            @Valid @RequestBody AfeccionRequest request,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.agregarAfeccion(usuarioId, request.afeccion());
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @DeleteMapping("/afecciones")
    @Operation(summary = "Quitar una afección o condición del perfil mediante body (HU2)",
               description = "Elimina dinámicamente una condición dermatológica del perfil del cliente.")
    public ResponseEntity<PerfilResponse> quitarAfeccionPorBody(
            @Valid @RequestBody AfeccionRequest request,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.quitarAfeccion(usuarioId, request.afeccion());
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

    @DeleteMapping("/afecciones/{afeccion}")
    @Operation(summary = "Quitar una afección o condición del perfil por parámetro de ruta (HU2)",
               description = "Elimina dinámicamente una condición dermatológica del perfil del cliente.")
    public ResponseEntity<PerfilResponse> quitarAfeccionPorRuta(
            @PathVariable String afeccion,
            Authentication authentication) {
        Long usuarioId = UsuarioAutenticado.id(authentication);
        PerfilDermatologico perfil = gestionarPerfilUseCase.quitarAfeccion(usuarioId, afeccion);
        return ResponseEntity.ok(PerfilResponse.fromDomain(perfil));
    }

}
