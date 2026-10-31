# Módulo: usuarios

Registro, autenticación (login) y gestión de roles. Es el módulo transversal de identidad
que emite los JWT consumidos por el resto de la aplicación.

**Proceso de negocio:** Transversal (habilita todos los demás).
**RNF relacionados:** RNF-11 (JWT), RNF-14 (control de acceso por rol).

## Estructura hexagonal
- `domain/model` → `Usuario`, `Rol`, `Credencial`
- `domain/port/in` → casos de uso: `RegistrarUsuarioUseCase`, `AutenticarUsuarioUseCase`
- `domain/port/out` → `UsuarioRepositoryPort`
- `application/service` → `RegistrarUsuarioService`, `AutenticarUsuarioService`
- `infrastructure/in/rest` → `UsuarioController` (+ `dto/`)
- `infrastructure/out/persistence` → entidad JPA, `UsuarioJpaRepository`, `UsuarioPersistenceAdapter`

## Tablas (ver V1__init.sql)
`usuario`, `rol`, `usuario_rol`

## Administración y recuperación de contraseña
- `AdministrarUsuariosService`: datos, rol, suspender y reactivar. Un administrador no puede suspenderse ni cambiarse el rol, así el sistema nunca se queda sin ninguno. Suspender a un vendedor bloquea sus productos con `Producto.MOTIVO_VENDEDOR_SUSPENDIDO`; reactivarlo devuelve solo esos.
- `SesionVigenteAdapter`: el filtro JWT consulta en cada petición si la cuenta sigue activa y si el token es posterior a `sesiones_desde`, para cortar de inmediato a un usuario suspendido o con el rol cambiado.
- `RecuperarClaveService`: enlace de un solo uso (1 hora por correo, 24 horas generado por el administrador). Solo se guarda el hash del token, y el administrador nunca ve ni fija la contraseña. Sin `SPRING_MAIL_*` el correo no sale: el enlace se entrega desde el panel.
