package com.glossup.catalogo.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad de dominio (raíz de agregado): producto cosmético del catálogo.
 *
 * <p>Reglas de negocio:</p>
 * <ul>
 *   <li>Nombre obligatorio; precio y stock nunca negativos.</li>
 *   <li>RNF-04: para publicarse (activarse) el producto debe tener ficha INCI (≥ 1 ingrediente).</li>
 *   <li>Al registrarse con su ficha INCI reglamentaria y lista de imágenes, inicia activo.</li>
 *   <li>El stock se reduce al vender y no puede quedar negativo (HU9 reserva de inventario).</li>
 * </ul>
 */
public class Producto {

    private final Long id;
    private final Long vendedorId;
    private String nombre;
    private String marca;
    private String categoria;
    private Dinero precio;
    private int stock;
    private Double ph;
    private String registroInvima;
    private boolean activo;
    private String descripcion;
    private final List<ImagenProducto> imagenes;
    private final List<ComposicionProducto> composicion;
    private final OffsetDateTime creadoEn;

    /** Constructor de reconstrucción completo (ej. desde persistencia). */
    public Producto(Long id, Long vendedorId, String nombre, String marca, String categoria,
                    Dinero precio, int stock, Double ph, String registroInvima, boolean activo,
                    List<ImagenProducto> imagenes, List<ComposicionProducto> composicion,
                    OffsetDateTime creadoEn) {
        this.id = id;
        this.vendedorId = vendedorId;
        this.nombre = validarNombre(nombre);
        this.marca = normalizarOpcional(marca);
        this.categoria = normalizarOpcional(categoria);
        this.precio = Objects.requireNonNull(precio, "precio requerido");
        this.stock = validarStock(stock);
        this.ph = ph;
        this.registroInvima = normalizarOpcional(registroInvima);
        this.activo = activo;
        this.imagenes = new ArrayList<>(imagenes == null ? List.of() : imagenes);
        this.composicion = new ArrayList<>();
        if (composicion != null) {
            for (ComposicionProducto item : composicion) {
                agregarIngrediente(item);
            }
        }
        this.creadoEn = creadoEn != null ? creadoEn : OffsetDateTime.now();
    }

    /** Constructor sobrecargado para retrocompatibilidad sin imágenes explícitas. */
    public Producto(Long id, Long vendedorId, String nombre, String marca, String categoria,
                    Dinero precio, int stock, Double ph, String registroInvima, boolean activo,
                    List<ComposicionProducto> composicion, OffsetDateTime creadoEn) {
        this(id, vendedorId, nombre, marca, categoria, precio, stock, ph, registroInvima, activo,
                List.of(), composicion, creadoEn);
    }

    /**
     * Fábrica para registrar un producto nuevo de forma atómica (HU3):
     * Requiere obligatoriamente ficha INCI con al menos un ingrediente.
     * Nace activo al cumplir RNF-04.
     */
    public static Producto registrar(Long vendedorId, String nombre, String marca, String categoria,
                                     Dinero precio, int stock, Double ph, String registroInvima,
                                     List<ImagenProducto> imagenes, List<ComposicionProducto> composicion) {
        if (composicion == null || composicion.isEmpty()) {
            throw new ReglaNegocioException("La ficha técnica debe contener al menos un ingrediente (INCI)");
        }
        return new Producto(null, vendedorId, nombre, marca, categoria, precio, stock, ph,
                registroInvima, true, imagenes, composicion, OffsetDateTime.now());
    }

    /** Fábrica inicial para registrar producto preliminar (inactivo sin ficha). */
    public static Producto registrar(Long vendedorId, String nombre, String marca, String categoria,
                                     Dinero precio, int stock, Double ph, String registroInvima) {
        return new Producto(null, vendedorId, nombre, marca, categoria, precio, stock, ph,
                registroInvima, false, new ArrayList<>(), new ArrayList<>(), OffsetDateTime.now());
    }

    // ---- Ficha INCI ----

    public void agregarIngrediente(ComposicionProducto item) {
        Objects.requireNonNull(item, "composición requerida");
        boolean yaEsta = composicion.stream()
                .anyMatch(c -> c.ingrediente().getInci().equals(item.ingrediente().getInci()));
        if (yaEsta) {
            throw new ReglaNegocioException(
                    "El ingrediente ya está en la ficha: " + item.ingrediente().getInci());
        }
        composicion.add(item);
    }

    public boolean contieneIngrediente(Inci inci) {
        return composicion.stream().anyMatch(c -> c.ingrediente().getInci().equals(inci));
    }

    // ---- Imágenes ----

    public void agregarImagen(ImagenProducto imagen) {
        Objects.requireNonNull(imagen, "imagen requerida");
        this.imagenes.add(imagen);
    }

    // ---- Publicación (RNF-04) ----

    /** Publica el producto. Regla: no se puede publicar sin ficha INCI. */
    public void publicar() {
        if (composicion.isEmpty()) {
            throw new ReglaNegocioException(
                    "No se puede publicar un producto sin ficha de ingredientes (INCI)");
        }
        this.activo = true;
    }

    public void despublicar() {
        this.activo = false;
    }

    // ---- Inventario (HU9) ----

    public void reducirStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad a reducir debe ser positiva");
        }
        if (cantidad > stock) {
            throw new ReglaNegocioException(
                    "Stock insuficiente para '" + nombre + "' (disponible: " + stock + ")");
        }
        this.stock -= cantidad;
    }

    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad a reponer debe ser positiva");
        }
        this.stock += cantidad;
    }

    public boolean hayStock(int cantidad) {
        return stock >= cantidad;
    }

    // ---- Edición y Eliminación (HU4) ----

    public void actualizarDatos(String nombre, String marca, String categoria,
                                Dinero precio, Double ph, String registroInvima) {
        this.nombre = validarNombre(nombre);
        this.marca = normalizarOpcional(marca);
        this.categoria = normalizarOpcional(categoria);
        this.precio = Objects.requireNonNull(precio, "precio requerido");
        this.ph = ph;
        this.registroInvima = normalizarOpcional(registroInvima);
    }

    /**
     * Actualiza de forma integral el producto (HU4): datos generales, stock,
     * lista de imágenes y ficha técnica INCI (cumpliendo RNF-04: >= 1 ingrediente).
     */
    public void actualizar(String nombre, String marca, String categoria,
                           Dinero precio, int stock, Double ph, String registroInvima,
                           List<ImagenProducto> nuevasImagenes,
                           List<ComposicionProducto> nuevaComposicion) {
        if (nuevaComposicion == null || nuevaComposicion.isEmpty()) {
            throw new ReglaNegocioException("La ficha técnica debe contener al menos un ingrediente (INCI)");
        }
        this.nombre = validarNombre(nombre);
        this.marca = normalizarOpcional(marca);
        this.categoria = normalizarOpcional(categoria);
        this.precio = Objects.requireNonNull(precio, "precio requerido");
        this.stock = validarStock(stock);
        this.ph = ph;
        this.registroInvima = normalizarOpcional(registroInvima);

        this.imagenes.clear();
        if (nuevasImagenes != null) {
            for (ImagenProducto imagen : nuevasImagenes) {
                agregarImagen(imagen);
            }
        }

        this.composicion.clear();
        for (ComposicionProducto item : nuevaComposicion) {
            agregarIngrediente(item);
        }
    }

    /** Verifica si el producto pertenece al vendedor indicado (RNF-14). */
    public boolean perteneceAVendedor(Long idVendedor) {
        return this.vendedorId != null && this.vendedorId.equals(idVendedor);
    }

    // ---- Validaciones ----

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del producto es obligatorio");
        }
        return nombre.trim();
    }

    private static int validarStock(int stock) {
        if (stock < 0) {
            throw new ReglaNegocioException("El stock no puede ser negativo");
        }
        return stock;
    }

    private static String normalizarOpcional(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ---- Accesores ----

    public Long getId() { return id; }
    public Long getVendedorId() { return vendedorId; }
    public String getNombre() { return nombre; }
    public String getMarca() { return marca; }
    public String getCategoria() { return categoria; }
    public Dinero getPrecio() { return precio; }
    public int getStock() { return stock; }
    public Double getPh() { return ph; }
    public String getRegistroInvima() { return registroInvima; }
    public boolean isActivo() { return activo; }

    public String getDescripcion() { return descripcion; }

    /** Descripción comercial que redacta el vendedor (opcional, hasta 2000 caracteres). */
    public void describir(String descripcion) {
        String d = normalizarOpcional(descripcion);
        if (d != null && d.length() > 2000) {
            throw new ReglaNegocioException("La descripción no puede superar 2000 caracteres");
        }
        this.descripcion = d;
    }

    /** Regla de negocio: el cliente solo ve en el catálogo lo que puede comprar (activo y con stock). */
    public boolean estaEnCatalogo() { return activo && stock > 0; }
    public List<ImagenProducto> getImagenes() { return List.copyOf(imagenes); }
    public List<ComposicionProducto> getComposicion() { return List.copyOf(composicion); }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}

