package com.glossup.perfil.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "perfil_afeccion")
public class PerfilAfeccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilDermatologicoEntity perfil;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    public PerfilAfeccionEntity() {}

    public PerfilAfeccionEntity(PerfilDermatologicoEntity perfil, String nombre) {
        this.perfil = perfil;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PerfilDermatologicoEntity getPerfil() { return perfil; }
    public void setPerfil(PerfilDermatologicoEntity perfil) { this.perfil = perfil; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
