# Módulo: catalogo

Gestión del catálogo de productos y la ontología de ingredientes (lista INCI). Estructura e
indexa la información química que consume el motor de compatibilidad dermatológica.

**Proceso de negocio:** Gestión de catálogo y ontología de ingredientes.

## Historias de usuario (Jira)
- **SCRUM-30 · HU3** — Registro Manual de Producto, Ficha INCI e Imágenes (5 pts) · Sprint 2 · **Completada**
- **SCRUM-31 · HU4** — Modificación y Eliminación de Productos (3 pts) · Sprint 2 · **Completada**
- **SCRUM-40 · HU12** — Búsqueda Básica de Productos por Palabra Clave (3 pts) · Sprint 2
- **SCRUM-32 · HU5** — Lectura OCR Asistida de Ingredientes desde Imagen (8 pts) · Sprint 3
- **SCRUM-41 · HU13** — Filtrado Avanzado por Atributos Dermatológicos (5 pts) · Sprint 3

**RNF relacionados:**
- **RNF-04**: Ficha INCI obligatoria (al menos un ingrediente al momento de registro y modificación).
- **RNF-11 / RNF-14**: Autenticación JWT, autorización con rol `VENDEDOR` y verificación de propiedad (`vendedorId`).
- Registro INVIMA obligatorio para venta legal.

## Estructura hexagonal detallada

```
catalogo/
├── domain/
│   ├── model/
│   │   ├── Producto.java               # Raíz de agregado (valida ficha INCI >= 1 ingrediente, edición y despublicación)
│   │   ├── ImagenProducto.java         # Value Object (hipervínculo de imagen)
│   │   ├── ComposicionProducto.java    # Value Object (ingrediente + concentración %)
│   │   ├── Ingrediente.java            # Entidad ontológica global
│   │   └── Inci.java                   # Value Object (nombre INCI normalizado)
│   └── port/
│       ├── in/
│       │   ├── RegistrarProductoUseCase.java
│       │   ├── RegistrarProductoCommand.java
│       │   ├── GestionarProductoUseCase.java
│       │   └── ActualizarProductoCommand.java
│       └── out/
│           ├── ProductoRepositoryPort.java
│           └── IngredienteRepositoryPort.java
├── application/
│   └── service/
│       ├── RegistrarProductoService.java
│       └── GestionarProductoService.java
└── infrastructure/
    ├── in/
    │   └── rest/
    │       ├── ProductoController.java
    │       └── dto/
    │           ├── RegistrarProductoRequest.java
    │           ├── ActualizarProductoRequest.java
    │           ├── IngredienteRequest.java
    │           └── ProductoResponse.java
    └── out/
        └── persistence/
            ├── entity/
            │   ├── ProductoEntity.java
            │   ├── ImagenProductoEntity.java
            │   ├── IngredienteEntity.java
            │   ├── ProductoIngredienteEntity.java
            │   └── ProductoIngredienteId.java
            ├── repository/
            │   ├── ProductoJpaRepository.java
            │   └── IngredienteJpaRepository.java
            └── adapter/
                ├── ProductoPersistenceAdapter.java
                └── IngredientePersistenceAdapter.java
```

## Tablas de Base de Datos
- `producto`: Tabla principal del catálogo.
- `producto_imagen`: Lista de hipervínculos de imágenes (1 a N, migración `V2__producto_imagenes.sql`).
- `ingrediente`: Ontología de ingredientes INCI únicos.
- `producto_ingrediente`: Composición del producto y concentraciones porcentuales.
