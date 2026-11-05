-- Restricciones de dominio en la base de datos (auditoría OBS-T04 / R-05): la consistencia no
-- depende solo de las validaciones de Java (scripts manuales, compras concurrentes).
-- NOT VALID: se exigen en toda fila nueva o modificada, pero no revisan las filas que ya existen,
-- para que un dato viejo fuera de rango en producción no impida arrancar la aplicación.

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_stock  CHECK (stock >= 0) NOT VALID,
    ADD CONSTRAINT chk_producto_precio CHECK (precio >= 0) NOT VALID,
    ADD CONSTRAINT chk_producto_ph     CHECK (ph IS NULL OR ph BETWEEN 0 AND 14) NOT VALID;

ALTER TABLE perfil_dermatologico
    ADD CONSTRAINT chk_perfil_ph CHECK (ph IS NULL OR ph BETWEEN 0 AND 14) NOT VALID;

ALTER TABLE pedido_item
    ADD CONSTRAINT chk_pedido_item_cantidad CHECK (cantidad > 0) NOT VALID,
    ADD CONSTRAINT chk_pedido_item_precio   CHECK (precio_unit >= 0) NOT VALID;

ALTER TABLE carrito_item
    ADD CONSTRAINT chk_carrito_item_cantidad CHECK (cantidad > 0) NOT VALID;
