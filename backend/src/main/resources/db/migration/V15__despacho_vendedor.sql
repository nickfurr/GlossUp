-- El vendedor confirma que entregó su paquete a la transportadora. Va por vendedor porque un pedido
-- puede tener productos de varios: pasa a "En camino" cuando todos confirmaron.
CREATE TABLE IF NOT EXISTS despacho_vendedor (
    pedido_id   BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    vendedor_id BIGINT NOT NULL REFERENCES usuario(id),
    enviado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (pedido_id, vendedor_id)
);
CREATE INDEX IF NOT EXISTS idx_despacho_vendedor ON despacho_vendedor (vendedor_id);
