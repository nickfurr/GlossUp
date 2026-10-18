package com.glossup.usuarios.infrastructure.out.persistence.entity;

import com.glossup.usuarios.domain.model.Rol;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;
    private String nombre;
    private boolean activo;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<RolEntity> roles = new HashSet<>();

    public UsuarioEntity() {}

    public UsuarioEntity(Long id, String email, String passwordHash, String nombre,
                         boolean activo, OffsetDateTime creadoEn, Set<RolEntity> roles) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activo = activo;
        this.creadoEn = creadoEn;
        if (roles != null) {
            this.roles = new HashSet<>(roles);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public Set<RolEntity> getRoles() { return roles; }
}
