-- Moderación del administrador: productos bloqueados, cuentas suspendidas y recuperación de contraseña.

-- Bloquear no toca "activo": al desbloquear, el producto vuelve como lo tenía el vendedor.
ALTER TABLE producto ADD COLUMN IF NOT EXISTS baneado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE producto ADD COLUMN IF NOT EXISTS motivo_baneo VARCHAR(300);

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS motivo_suspension VARCHAR(300);
-- Los tokens emitidos antes de esta fecha dejan de valer (rol cambiado o contraseña restablecida).
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS sesiones_desde TIMESTAMPTZ;

-- Solo se guarda el hash del enlace: quien lea la base no puede usarlo.
CREATE TABLE IF NOT EXISTS recuperacion_clave (
    token_hash CHAR(64) PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en  TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recuperacion_usuario ON recuperacion_clave (usuario_id);
