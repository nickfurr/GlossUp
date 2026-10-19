package com.glossup.usuarios.domain.port.in;

import com.glossup.usuarios.domain.model.Usuario;

public interface RegistrarUsuarioUseCase {

    Usuario registrar(RegistrarUsuarioCommand command);
}
