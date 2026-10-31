package com.glossup.catalogo.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto")
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendedor_id")
    private Long vendedorId;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "marca", length = 150)
    private String marca;

    @Column(name = "categoria", length = 120)
    private String categoria;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "ph")
    private Double ph;

    @Column(name = "registro_invima", length = 100)
    private String registroInvima;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "baneado", nullable = false)
    private boolean baneado;

    @Column(name = "motivo_baneo", length = 300)
    private String motivoBaneo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<ImagenProductoEntity> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductoIngredienteEntity> composicion = new ArrayList<>();

    public ProductoEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVendedorId() { return vendedorId; }
    public void setVendedorId(Long vendedorId) { this.vendedorId = vendedorId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }

    public String getRegistroInvima() { return registroInvima; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setRegistroInvima(String registroInvima) { this.registroInvima = registroInvima; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isBaneado() { return baneado; }
    public void setBaneado(boolean baneado) { this.baneado = baneado; }
    public String getMotivoBaneo() { return motivoBaneo; }
    public void setMotivoBaneo(String motivoBaneo) { this.motivoBaneo = motivoBaneo; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

    public List<ImagenProductoEntity> getImagenes() { return imagenes; }
    public void setImagenes(List<ImagenProductoEntity> imagenes) { this.imagenes = imagenes; }

    public List<ProductoIngredienteEntity> getComposicion() { return composicion; }
    public void setComposicion(List<ProductoIngredienteEntity> composicion) { this.composicion = composicion; }

    public void agregarImagen(ImagenProductoEntity img) {
        imagenes.add(img);
        img.setProducto(this);
    }

    public void agregarIngrediente(ProductoIngredienteEntity pi) {
        composicion.add(pi);
        pi.setProducto(this);
    }
}
