-- =====================================================================================
-- MODULO: recomendacion / ventas - Carrito de compras interactivo (HU9 - CRUD de carrito)
-- Un carrito activo por usuario; sus lineas capturan el precio al momento de agregarlas.
-- =====================================================================================

CREATE TABLE carrito (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE carrito_item (
    id          BIGSERIAL PRIMARY KEY,
    carrito_id  BIGINT NOT NULL REFERENCES carrito(id)  ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    cantidad    INT    NOT NULL,
    precio_unit NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_carrito_producto UNIQUE (carrito_id, producto_id)
);
