package com.glossup.usuarios.infrastructure.out.persistence.adapter;

import com.glossup.usuarios.domain.model.Rol;
import com.glossup.usuarios.domain.model.Usuario;
import com.glossup.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.glossup.usuarios.infrastructure.out.persistence.entity.UsuarioEntity;
import com.glossup.usuarios.infrastructure.out.persistence.repository.UsuarioJpaRepository;
import com.glossup.usuarios.infrastructure.out.persistence.repository.RolJpaRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repository;
    private final RolJpaRepository rolRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository repository, RolJpaRepository rolRepository) {
        this.repository = repository;
        this.rolRepository = rolRepository;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(this::aDominio);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(usuario.getId()); // con id actualiza la cuenta existente; sin id la crea
        entity.setEmail(usuario.getEmail());
        entity.setPasswordHash(usuario.getPasswordHash());
        entity.setNombre(usuario.getNombre());
        entity.setActivo(usuario.isActivo());
        entity.setCreadoEn(usuario.getCreadoEn());
        entity.setMotivoSuspension(usuario.getMotivoSuspension());
        entity.setSesionesDesde(usuario.getSesionesDesde());
        entity.setRoles(usuario.getRoles().stream()
                .map(rol -> rol.name())
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + nombre)))
                .collect(Collectors.toSet()));
        return aDominio(repository.save(entity));
    }

    private Usuario aDominio(UsuarioEntity entity) {
        EnumSet<Rol> roles = EnumSet.noneOf(Rol.class);
        entity.getRoles().forEach(rol -> roles.add(Rol.valueOf(rol.getNombre())));
        return new Usuario(entity.getId(), entity.getEmail(), entity.getNombre(),
                entity.getPasswordHash(), entity.isActivo(), roles, entity.getCreadoEn())
                .conModeracion(entity.getMotivoSuspension(), entity.getSesionesDesde());
    }
}
