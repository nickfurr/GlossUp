package com.glossup.perfil.application.service;

import com.glossup.perfil.domain.model.PerfilDermatologico;
import com.glossup.perfil.domain.port.in.GestionarPerfilUseCase;
import com.glossup.perfil.domain.port.out.PerfilRepositoryPort;
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
    public PerfilDermatologico agregarAlergia(Long usuarioId, String alergia) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.agregarAlergia(alergia);
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico quitarAlergia(Long usuarioId, String alergia) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.quitarAlergia(alergia);
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico agregarAfeccion(Long usuarioId, String afeccion) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.agregarAfeccion(afeccion);
        return perfilRepository.guardar(perfil);
    }

    @Override
    public PerfilDermatologico quitarAfeccion(Long usuarioId, String afeccion) {
        PerfilDermatologico perfil = obtenerOCrearDominio(usuarioId);
        perfil.quitarAfeccion(afeccion);
        return perfilRepository.guardar(perfil);
    }

    private PerfilDermatologico obtenerOCrearDominio(Long usuarioId) {
        return perfilRepository.buscarPorUsuarioId(usuarioId)
                .orElseGet(() -> PerfilDermatologico.crear(usuarioId, null, null, null));
    }
}
