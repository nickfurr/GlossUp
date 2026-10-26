package com.glossup.compatibilidad.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Regla ingrediente↔condición: un ingrediente (INCI) desaconsejado para un tipo de piel o afección.
 * Es conocimiento dermatológico independiente del inventario, por eso guarda el INCI por nombre.
 */
@Entity
@Table(name = "regla_perfil")
public class ReglaPerfilEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingrediente_inci", nullable = false, length = 200)
    private String ingredienteInci;

    @Column(name = "condicion", nullable = false, length = 120)
    private String condicion;

    @Column(name = "severidad", nullable = false, length = 20)
    private String severidad;

    // TEXT como en la migración; sin esto ddl-auto=update lo reduce a varchar(255).
    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    public ReglaPerfilEntity() {}

    public Long getId() { return id; }
    public String getIngredienteInci() { return ingredienteInci; }
    public String getCondicion() { return condicion; }
    public String getSeveridad() { return severidad; }
    public String getMotivo() { return motivo; }
}
