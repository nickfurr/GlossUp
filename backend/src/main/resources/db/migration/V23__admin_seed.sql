-- =====================================================================================
-- GlossUP - Migración V23: Usuario administrador por defecto para auditoría y gestión
-- Permite autenticarse como ADMIN para endpoints de administración y auditoría de RNF.
-- Credenciales por defecto: admin@glossup.co / Admin123!
-- =====================================================================================

INSERT INTO usuario (email, password_hash, nombre, activo)
VALUES ('admin@glossup.co', '$2a$10$6epLKuqpZW9FCyj86UORYuRJsNv8OgFh6QO3M8aDjfiEdKY6QR7Eu', 'Administrador GlossUP', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u
JOIN rol r ON r.nombre = 'ADMIN'
WHERE lower(u.email) = 'admin@glossup.co'
ON CONFLICT DO NOTHING;
