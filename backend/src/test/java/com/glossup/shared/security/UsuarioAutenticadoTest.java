package com.glossup.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioAutenticadoTest {

    @Test
    void subjectNumerico_devuelveElId() {
        assertThat(UsuarioAutenticado.id(new UsernamePasswordAuthenticationToken("42", null))).isEqualTo(42L);
    }

    @Test
    void sinSesion_fallaEnVezDeSuponerElUsuario1() {
        assertThatThrownBy(() -> UsuarioAutenticado.id(null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subjectNoNumerico_fallaEnVezDeUsarHashCode() {
        assertThatThrownBy(() -> UsuarioAutenticado.id(new UsernamePasswordAuthenticationToken("ana@correo.com", null)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
