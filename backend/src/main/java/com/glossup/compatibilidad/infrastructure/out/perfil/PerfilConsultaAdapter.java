package com.glossup.compatibilidad.infrastructure.out.perfil;

import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Único punto de acoplamiento compatibilidad → perfil. Expone alergias y condiciones (afecciones
 * + tipo de piel) ya normalizadas; si el usuario no tiene perfil, devuelve conjuntos vacíos.
 */
@Component
public class PerfilConsultaAdapter implements PerfilConsultaPort {

    private final PerfilRepositoryPort perfilRepository;

    public PerfilConsultaAdapter(PerfilRepositoryPort perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Override
    public PerfilConsulta obtener(Long usuarioId) {
        return perfilRepository.buscarPorUsuarioId(usuarioId)
                .map(this::aConsulta)
                .orElseGet(() -> new PerfilConsulta(Set.of(), Set.of()));
    }

    private PerfilConsulta aConsulta(PerfilDermatologico p) {
        Set<String> condiciones = new HashSet<>(p.getAfecciones()); // ya en minúsculas
        TipoPiel tipoPiel = p.getTipoPiel();
        if (tipoPiel != null) condiciones.add(tipoPiel.name().toLowerCase());
        return new PerfilConsulta(p.getAlergias(), condiciones);
    }
}
