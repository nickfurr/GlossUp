package com.glossup.perfil.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perfil_dermatologico")
public class PerfilDermatologicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(name = "ph", precision = 3, scale = 1)
    private BigDecimal ph;

    @Column(name = "tipo_piel", length = 20)
    private String tipoPiel;

    @Column(name = "tipo_cabello", length = 30)
    private String tipoCabello;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PerfilAfeccionEntity> afecciones = new ArrayList<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PerfilAlergiaEntity> alergias = new ArrayList<>();

    public PerfilDermatologicoEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public BigDecimal getPh() { return ph; }
    public void setPh(BigDecimal ph) { this.ph = ph; }

    public String getTipoPiel() { return tipoPiel; }
    public void setTipoPiel(String tipoPiel) { this.tipoPiel = tipoPiel; }

    public String getTipoCabello() { return tipoCabello; }
    public void setTipoCabello(String tipoCabello) { this.tipoCabello = tipoCabello; }

    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public List<PerfilAfeccionEntity> getAfecciones() { return afecciones; }
    public void setAfecciones(List<PerfilAfeccionEntity> afecciones) { this.afecciones = afecciones; }

    public List<PerfilAlergiaEntity> getAlergias() { return alergias; }
    public void setAlergias(List<PerfilAlergiaEntity> alergias) { this.alergias = alergias; }

    public void agregarAfeccion(PerfilAfeccionEntity afeccion) {
        afecciones.add(afeccion);
        afeccion.setPerfil(this);
    }

    public void agregarAlergia(PerfilAlergiaEntity alergia) {
        alergias.add(alergia);
        alergia.setPerfil(this);
    }
}
