-- HU10 · Pago del pedido con pasarela externa (Wompi).
-- Solo se guarda la referencia de la transacción, nunca datos de la tarjeta (RNF-13).

ALTER TABLE pedido ADD COLUMN IF NOT EXISTS pago_transaccion_id VARCHAR(64);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS pago_metodo VARCHAR(40);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS pagado_en TIMESTAMPTZ;

-- El trabajo que cancela pedidos sin pagar busca por estado y antigüedad.
CREATE INDEX IF NOT EXISTS idx_pedido_estado_creado ON pedido (estado, creado_en);

-- Bloqueo optimista: si dos procesos tocan el mismo pedido a la vez (webhook y regreso del cliente,
-- o la tarea de vencidos en dos réplicas), el segundo falla y no se libera el stock dos veces.
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
