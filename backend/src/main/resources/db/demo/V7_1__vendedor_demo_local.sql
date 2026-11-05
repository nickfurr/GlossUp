-- Solo en el perfil docker (base local, ver application-docker.yml): crea el vendedor de demostración que busca V8,
-- para que un arranque limpio tenga catálogo (auditoría OBS-RES-02). Producción no carga esta carpeta.
-- Sin contraseña utilizable: es el dueño de los productos de demostración, no una cuenta para entrar.
INSERT INTO usuario (email, password_hash, nombre, activo)
VALUES ('manuel.pinedave@uqvirtual.edu.co', '!sin-clave-cuenta-de-demostracion', 'Tienda demo GlossUP', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.nombre = 'VENDEDOR'
WHERE lower(u.email) = 'manuel.pinedave@uqvirtual.edu.co'
ON CONFLICT DO NOTHING;
