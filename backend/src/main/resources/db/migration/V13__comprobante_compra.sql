-- HU11: comprobante de compra (soporte digital del pago).

-- La línea del pedido guarda el nombre del producto al comprar: si el vendedor lo renombra o lo
-- retira, el historial y el comprobante siguen mostrando lo que el cliente compró.
ALTER TABLE pedido_item ADD COLUMN IF NOT EXISTS nombre VARCHAR(200);
UPDATE pedido_item i SET nombre = p.nombre FROM producto p WHERE p.id = i.producto_id AND i.nombre IS NULL;

-- PDF emitido una sola vez al confirmarse el pago; se guarda tal cual para que el soporte no
-- cambie aunque cambien los productos o los datos del cliente.
CREATE TABLE IF NOT EXISTS comprobante (
    pedido_id  BIGINT PRIMARY KEY REFERENCES pedido(id) ON DELETE CASCADE,
    numero     VARCHAR(30) NOT NULL UNIQUE,
    pdf        BYTEA NOT NULL,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
