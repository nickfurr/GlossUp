# Módulo: perfil

Captura y mantiene el perfil dermatológico del cliente (pH, tipo de piel, tipo de cabello,
afecciones, alergias). Es la entrada que alimenta al motor de compatibilidad.

**Proceso de negocio:** Gestión del perfil dermatológico.

## Historias de usuario (Jira — Sprint 2)
- **SCRUM-28 · HU1** — Registro Inicial de Perfil Dermatológico (3 pts)
- **SCRUM-29 · HU2** — Gestión Dinámica de Alergias y Condiciones (Agregar/Quitar) (3 pts) · **Completada**

**RNF relacionados:** RNF-02 (completitud del análisis), RNF-08/09/10 (usabilidad y validación de datos), RNF-11/14 (JWT + rol `CLIENTE`).

## Estructura hexagonal detallada
```
perfil/
├── domain/
│   ├── model/
│   │   ├── PerfilDermatologico.java       # Raíz del agregado (alergias y afecciones normalizadas)
│   │   ├── Ph.java                        # Value Object (escala 0–14)
│   │   └── TipoPiel.java                  # Enum (NORMAL, SECA, GRASA, MIXTA, SENSIBLE)
│   └── port/
│       ├── in/
│       │   └── GestionarPerfilUseCase.java
│       └── out/
│           └── PerfilRepositoryPort.java
├── application/
│   └── service/
│       └── GestionarPerfilService.java
└── infrastructure/
    ├── in/
    │   └── rest/
    │       ├── PerfilController.java
    │       └── dto/
    │           ├── AlergiaRequest.java
    │           ├── AfeccionRequest.java
    │           └── PerfilResponse.java
    └── out/
        └── persistence/
            ├── entity/
            │   ├── PerfilDermatologicoEntity.java
            │   ├── PerfilAfeccionEntity.java
            │   └── PerfilAlergiaEntity.java
            ├── repository/
            │   └── PerfilJpaRepository.java
            └── adapter/
                └── PerfilPersistenceAdapter.java
```

## Endpoints REST (`/api/perfil/me`)
- `GET /api/perfil/me` — Obtiene el perfil del cliente autenticado (crea uno vacío si no existe).
- `POST /api/perfil/me/alergias` — Agrega dinámicamente una alergia (INCI) al perfil.
- `DELETE /api/perfil/me/alergias` y `DELETE /api/perfil/me/alergias/{alergia}` — Quita una alergia del perfil.
- `POST /api/perfil/me/afecciones` — Agrega dinámicamente una afección o condición dermatológica.
- `DELETE /api/perfil/me/afecciones` y `DELETE /api/perfil/me/afecciones/{afeccion}` — Quita una afección del perfil.

## Tablas (ver V1__init.sql)
`perfil_dermatologico`, `perfil_afeccion`, `perfil_alergia`
