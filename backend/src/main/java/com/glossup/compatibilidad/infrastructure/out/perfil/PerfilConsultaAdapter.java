package com.glossup.compatibilidad.infrastructure.out.perfil;

import com.glossup.compatibilidad.domain.port.out.PerfilConsultaPort;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Único punto de acoplamiento compatibilidad → perfil. Expone los códigos de alérgenos y las
 * condiciones (afecciones + tipo de piel) en minúsculas; sin perfil, conjuntos vacíos.
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
        Map<String, String> etiquetas = new HashMap<>();
        p.getAlergias().forEach(a -> etiquetas.put(a.name().toLowerCase(), a.getEtiqueta()));
        p.getAfecciones().forEach(a -> etiquetas.put(a.name().toLowerCase(), a.getEtiqueta()));
        Set<String> condiciones = new HashSet<>(codigos(p.getAfecciones()));
        if (p.getTipoPiel() != null) {
            String piel = p.getTipoPiel().name().toLowerCase();
            condiciones.add(piel);
            etiquetas.put(piel, "Piel " + piel);
        }
        return new PerfilConsulta(codigos(p.getAlergias()), condiciones, etiquetas);
    }

    private static Set<String> codigos(Set<? extends Enum<?>> valores) {
        return valores.stream().map(v -> v.name().toLowerCase()).collect(Collectors.toSet());
    }
}
