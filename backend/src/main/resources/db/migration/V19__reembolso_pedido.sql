-- Pago aprobado que llega a un pedido ya cancelado (auditoría OBS-T13): el pedido guarda la transacción
-- (pago_transaccion_id) y queda con reembolso pendiente hasta que el administrador lo devuelve.
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS reembolsado_en TIMESTAMPTZ;

-- El panel del administrador busca los reembolsos pendientes.
CREATE INDEX IF NOT EXISTS idx_pedido_reembolso_pendiente ON pedido (id)
    WHERE estado = 'CANCELADO' AND pago_transaccion_id IS NOT NULL AND reembolsado_en IS NULL;
