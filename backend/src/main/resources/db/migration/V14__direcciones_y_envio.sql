-- HU14: libreta de direcciones del cliente y envío de cada pedido.

CREATE TABLE IF NOT EXISTS direccion (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT NOT NULL,
    nombre_recibe VARCHAR(120) NOT NULL,
    telefono      VARCHAR(10) NOT NULL,
    departamento  VARCHAR(40) NOT NULL,
    ciudad        VARCHAR(80) NOT NULL,
    barrio        VARCHAR(80) NOT NULL,
    direccion     VARCHAR(160) NOT NULL,
    complemento   VARCHAR(120),
    indicaciones  VARCHAR(300),
    principal     BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_direccion_usuario ON direccion (usuario_id);

-- El pedido copia la dirección al comprar: editar o borrar la libreta no cambia a dónde va lo ya comprado.
-- Los pedidos anteriores a esta versión no tienen envío (columnas en NULL).
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_nombre_recibe VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_telefono VARCHAR(10);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_departamento VARCHAR(40);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_ciudad VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_barrio VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_direccion VARCHAR(160);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_complemento VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_indicaciones VARCHAR(300);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_metodo VARCHAR(20);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_costo NUMERIC(12, 2);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_fecha_estimada DATE;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_estado VARCHAR(20);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_transportadora VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_guia VARCHAR(60);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_intentos INTEGER;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_notificado_en TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_enviado_en TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_entregado_en TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS envio_no_entregado_en TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pedido_envio_guia ON pedido (envio_guia) WHERE envio_guia IS NOT NULL;
-- Las tareas de reintento y seguimiento solo miran envíos sin terminar.
CREATE INDEX IF NOT EXISTS idx_pedido_envio_estado ON pedido (envio_estado) WHERE envio_estado IN ('PREPARANDO', 'LISTO_PARA_ENVIO', 'EN_CAMINO');
