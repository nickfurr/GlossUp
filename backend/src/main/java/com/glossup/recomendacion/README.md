# Módulo: recomendacion

Recomendación con IA, carrito de compras, checkout y orquestación con sistemas externos
(pasarela de pagos, API de envíos). Reúne la lógica de venta del producto validado.

**Proceso de negocio:** Gestión de ventas y checkout + recomendación inteligente.

## Historias de usuario (Jira — Sprint 3)
- **SCRUM-36 · HU9** — Consolidación de Carrito y Reserva de Inventario (5 pts)
- **SCRUM-37 · HU10** — Procesamiento de Pago Seguro mediante Pasarela Externa (5 pts)
- **SCRUM-38 · HU11** — Generación de Comprobante de Compra y Soporte Digital (3 pts)
- **SCRUM-39 · HU14** — Notificación Asíncrona a la API de Envíos (Fire and Forget) (3 pts)

**RNF relacionados:** RNF-13 (no almacenar datos de tarjeta), RNF-19 (timeouts/reintentos), RNF-22 (tolerancia a fallos externos), RNF-25 (checkout transaccional ACID).

## Estructura hexagonal
- `domain/model` → `Carrito`, `Pedido`, `PedidoItem`, `Recomendacion`
- `domain/port/in` → `GestionarCarritoUseCase`, `RealizarCheckoutUseCase`, `RecomendarUseCase`
- `domain/port/out` → `PedidoRepositoryPort`, `PasarelaPagosPort`, `EnvioPort`, `IARecomendacionPort`
- `application/service` → crear pedido, procesar pago, generar comprobante
- `infrastructure/in/rest` → `CheckoutController`, `RecomendacionController` (+ `dto/`)
- `infrastructure/out/persistence` → adaptadores JPA
- `infrastructure/out/pago` → `WompiPasarelaAdapter` (HU10): Web Checkout firmado, consulta de
  transacciones y verificación del checksum de eventos. Llaves por variables de entorno `WOMPI_*`.
- `infrastructure/in/tarea` → `PedidosVencidosTarea`: cada minuto cancela pedidos sin pagar vencidos
  (30 min + 5 de margen) y devuelve su inventario.
- Pendientes: `IARecomendacionClient` (motor de IA) y `EnvioApiClient` (API de envíos, HU14).

## Flujo de pago (HU10)
1. `POST /api/pedidos` reserva inventario (HU9) → pedido `CREADO`, vence a los 30 min.
2. `POST /api/pedidos/{id}/pago` → URL del checkout de Wompi (firma SHA256 de integridad).
3. El cliente paga en Wompi (GlossUP nunca ve la tarjeta, RNF-13) y vuelve a `/pago/resultado/{id}?id={tx}`.
4. `GET /api/pedidos/{id}/pago/{tx}` consulta la transacción a Wompi; el webhook
   `POST /api/pagos/wompi/eventos` (checksum con el secreto de eventos) hace lo mismo sin depender del cliente.
   Solo una transacción APROBADA con el mismo monto y moneda pasa el pedido a `PAGADO`; aplicarla dos veces no cambia nada.

## Tablas (ver V1__init.sql)
`pedido` (+ `pago_transaccion_id`, `pago_metodo`, `pagado_en`, V12), `pedido_item`
