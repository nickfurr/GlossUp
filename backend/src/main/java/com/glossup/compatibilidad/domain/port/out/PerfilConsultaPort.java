package com.glossup.compatibilidad.domain.port.out;

import java.util.Map;
import java.util.Set;

/**
 * Puerto de salida: datos del perfil dermatológico que el motor necesita, sin acoplarse al
 * agregado {@code PerfilDermatologico} del módulo de perfil.
 */
public interface PerfilConsultaPort {

    /** Perfil de un usuario. Si no tiene perfil, ambos conjuntos vienen vacíos. */
    PerfilConsulta obtener(Long usuarioId);

    /**
     * @param alergias   códigos de alérgeno declarados (ej. "parabenos"), en minúsculas
     * @param condiciones afecciones + tipo de piel del usuario, normalizados (para cruzar irritantes)
     * @param etiquetas   código (minúsculas) → nombre legible, para los mensajes al usuario (RNF-07)
     */
    record PerfilConsulta(Set<String> alergias, Set<String> condiciones, Map<String, String> etiquetas) {
        public PerfilConsulta(Set<String> alergias, Set<String> condiciones) {
            this(alergias, condiciones, Map.of());
        }

        public String etiqueta(String codigo) {
            return etiquetas.getOrDefault(codigo, codigo.replace('_', ' '));
        }
    }
}
