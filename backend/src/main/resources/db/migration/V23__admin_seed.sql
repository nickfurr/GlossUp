-- =====================================================================================
-- GlossUP - Migración V23: Usuario administrador por defecto para auditoría y gestión
-- Permite autenticarse como ADMIN para endpoints de administración y auditoría de RNF.
-- Credenciales por defecto: admin@glossup.co / Admin123!
-- =====================================================================================

-- Sincronizar la secuencia de id de usuario con el valor máximo actual para evitar colisiones
-- con registros que hayan sido insertados con id explícito (p.ej. tests o migraciones previas).
SELECT setval(
    pg_get_serial_sequence('usuario', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 0) FROM usuario), 1),
    (SELECT COUNT(*) > 0 FROM usuario)
);

INSERT INTO usuario (email, password_hash, nombre, activo)
SELECT 'admin@glossup.co', '$2a$10$6epLKuqpZW9FCyj86UORYuRJsNv8OgFh6QO3M8aDjfiEdKY6QR7Eu', 'Administrador GlossUP', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = 'admin@glossup.co')
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.nombre = 'ADMIN'
WHERE lower(u.email) = 'admin@glossup.co'
ON CONFLICT DO NOTHING;

