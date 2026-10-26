package com.glossup.catalogo.infrastructure.out.persistence.adapter;

import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.port.out.IngredienteRepositoryPort;
import com.glossup.catalogo.infrastructure.out.persistence.entity.IngredienteEntity;
import com.glossup.catalogo.infrastructure.out.persistence.repository.IngredienteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class IngredientePersistenceAdapter implements IngredienteRepositoryPort {

    private final IngredienteJpaRepository repository;

    public IngredientePersistenceAdapter(IngredienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Ingrediente> buscarPorInci(Inci inci) {
        return repository.findByNombreInciIgnoreCase(inci.nombre())
                .map(this::aDominio);
    }

    @Override
    public List<Ingrediente> buscar(String texto) {
        String t = texto.trim();
        return repository
                .findTop20ByNombreInciContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrderByNombreInciAsc(t, t)
                .stream().map(this::aDominio).toList();
    }

    public Ingrediente aDominio(IngredienteEntity entity) {
        return new Ingrediente(
                entity.getId(),
                new Inci(entity.getNombreInci()),
                entity.getDescripcion()
        );
    }

    public IngredienteEntity aEntity(Ingrediente dominio) {
        return new IngredienteEntity(
                dominio.getId(),
                dominio.getInci().nombre(),
                dominio.getDescripcion()
        );
    }
}
