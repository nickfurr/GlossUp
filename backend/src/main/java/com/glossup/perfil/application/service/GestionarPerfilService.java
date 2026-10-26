package com.glossup.perfil.application.service;

import com.glossup.perfil.domain.model.Afeccion;
import com.glossup.perfil.domain.model.Alergeno;
import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.model.Ph;
import com.glossup.perfil.domain.model.TipoCabello;
import com.glossup.perfil.domain.model.TipoPiel;
import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación que implementa {@link GestionarPerfilUseCase}.
 * Orquesta la obtención/creación perezosa del perfil dermatológico y la gestión
 * dinámica de alergias y afecciones (HU2 - SCRUM-29).
 */
@Service
@Transactional
public class GestionarPerfilService implements GestionarPerfilUseCase {

    private final PerfilRepositoryPort perfilRepository;

    public GestionarPerfilService(PerfilRepositoryPort perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    @Override
    public PerfilDermatologico obtenerPerfil(Long usuarioId) {
        return perfilRepository.buscarPorUsuarioId(usuarioId)
                .orElseGet(() -> perfilRepository.guardar(
                        PerfilDermatologico.crear(usuarioId, null, null, null)
                ));
    }

    @Override
    public PerfilDermatologico actualizarBiotipo(Long usuarioId, Double ph, String tipoPiel, String tipoCabello) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.actualizarBiotipo(ph == null ? null : new Ph(ph),
                opcional(TipoPiel.class, tipoPiel, "tipo de piel"),
                opcional(TipoCabello.class, tipoCabello, "tipo de cabello"));
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico agregarAlergia(Long usuarioId, String alergia) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.agregarAlergia(codigo(Alergeno.class, alergia, "alergia"));
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico quitarAlergia(Long usuarioId, String alergia) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.quitarAlergia(codigo(Alergeno.class, alergia, "alergia"));
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico agregarAfeccion(Long usuarioId, String afeccion) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.agregarAfeccion(codigo(Afeccion.class, afeccion, "afección"));
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico quitarAfeccion(Long usuarioId, String afeccion) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.quitarAfeccion(codigo(Afeccion.class, afeccion, "afección"));
        return perfilRepository.guardar(perfil);
    }

    private PerfilDermatologico obtenerOCrearDominio(Long usuarioId) {
        return perfilRepository.buscarPorUsuarioId(usuarioId)
                .orElseGet(() -> PerfilDermatologico.crear(usuarioId, null, null, null));
    }

    private static <E extends Enum<E>> E opcional(Class<E> tipo, String valor, String campo) {
        return valor == null || valor.isBlank() ? null : codigo(tipo, valor, campo);
    }

    /** Convierte el código recibido en el valor del catálogo cerrado; rechaza texto libre. */
    private static <E extends Enum<E>> E codigo(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El campo " + campo + " es obligatorio");
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Valor no válido para " + campo + ": " + valor);
        }
    }
}
