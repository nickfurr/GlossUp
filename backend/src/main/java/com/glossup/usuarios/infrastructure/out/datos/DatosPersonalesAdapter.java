package com.glossup.usuarios.infrastructure.out.datos;

import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.recomendacion.domain.port.in.GestionarDireccionesUseCase;
import com.glossup.usuarios.domain.port.out.DatosPersonalesPort;
import org.springframework.stereotype.Component;

@Component
public class DatosPersonalesAdapter implements DatosPersonalesPort {

    private final GestionarPerfilUseCase perfiles;
    private final GestionarDireccionesUseCase direcciones;

    public DatosPersonalesAdapter(GestionarPerfilUseCase perfiles, GestionarDireccionesUseCase direcciones) {
        this.perfiles = perfiles;
        this.direcciones = direcciones;
    }

    @Override
    public void suprimir(Long usuarioId) {
        perfiles.eliminarPerfil(usuarioId);
        // Los pedidos guardan su propia copia de la dirección de entrega, que se conserva con el comprobante.
        direcciones.listar(usuarioId).forEach(d -> direcciones.eliminar(usuarioId, d.id()));
    }
}
