-- Las cuentas VENDEDOR que se crean desde el registro público esperan la aprobación de un administrador
-- antes de poder entrar (auditoría OBS-T08 / R-12). Las cuentas que ya existen quedan aprobadas.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS pendiente_aprobacion BOOLEAN NOT NULL DEFAULT FALSE;
