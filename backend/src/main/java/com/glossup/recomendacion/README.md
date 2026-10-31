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
- `infrastructure/out/envio` → `EnvioApiAdapter` (API de envíos real, HU14) o `TransportadoraSimuladaAdapter` (demostración).
- Pendiente: `IARecomendacionClient` (motor de IA).

## Flujo de pago (HU10)
1. `POST /api/pedidos` reserva inventario (HU9) → pedido `CREADO`, vence a los 30 min.
2. `POST /api/pedidos/{id}/pago` → URL del checkout de Wompi (firma SHA256 de integridad).
3. El cliente paga en Wompi (GlossUP nunca ve la tarjeta, RNF-13) y vuelve a `/pago/resultado/{id}?id={tx}`.
4. `GET /api/pedidos/{id}/pago/{tx}` consulta la transacción a Wompi; el webhook
   `POST /api/pagos/wompi/eventos` (checksum con el secreto de eventos) hace lo mismo sin depender del cliente.
   Solo una transacción APROBADA con el mismo monto y moneda pasa el pedido a `PAGADO`; aplicarla dos veces no cambia nada.

## Comprobante de compra (HU11)
1. Al pasar a `PAGADO`, `PagarPedidoService` publica `PedidoPagado`; `ComprobanteAlPagarListener` lo atiende
   después del commit y en otro hilo, así el PDF y el correo no frenan ni revierten el pago.
2. `ComprobanteService` genera el PDF (`ComprobantePdfAdapter`, OpenPDF), lo guarda una sola vez en `comprobante`
   y lo envía por correo (`CorreoNotificacionAdapter`, SMTP por `SPRING_MAIL_*`; sin configurar no envía).
3. `GET /api/pedidos/{id}/comprobante` entrega siempre el mismo PDF; si la emisión automática falló, lo emite ahí.
4. El front muestra el historial en `/pedidos` (detalle, estado, pago y descarga).

## Checkout y envío (HU14)
El checkout sigue el de Mercado Libre: una sola página (`/checkout`) con la dirección principal ya elegida, formas de
envío con fecha y costo, resumen y "Pagar y finalizar". Los estados del envío siguen su modelo (handling, ready_to_ship,
shipped, delivered, not_delivered).
1. `/api/direcciones`: libreta del cliente (hasta 10, una principal). `GET /api/envios/opciones` cotiza por departamento.
2. `POST /api/pedidos` recibe `direccionId` y `metodoEnvio`: copia la dirección al pedido y suma el costo calculado en el
   servidor (`TarifaEnvio`) al total que cobra Wompi. Editar o borrar la libreta no cambia los pedidos hechos.
3. Al pasar a `PAGADO`, `EnvioListener` avisa a la API de envíos después del commit y en otro hilo (fire and forget). Si falla,
   el pago sigue intacto y `EnviosPendientesTarea` reintenta cada minuto, hasta 30 veces (RNF-19, RNF-22).
4. El vendedor confirma en su panel que entregó el paquete (`POST /api/vendedor/ventas/{id}/enviado`). Con todos los vendedores
   del pedido confirmados pasa a `EN_CAMINO`. No se puede confirmar hasta que la transportadora asigne la guía.
5. La transportadora reporta etapas en `POST /api/envios/eventos` con el secreto `X-Envios-Secreto`: `EN_CAMINO` pasa el
   pedido a `ENVIADO`; `ENTREGADO` y `NO_ENTREGADO` son finales. Los eventos repetidos no cambian nada. Cada cambio avisa por correo.

| Variable | Para qué |
|---|---|
| `GLOSSUP_ENVIOS_URL` | API de la transportadora: `POST {url}/envios` responde `{transportadora, guia}`. Sin ella se usa la transportadora de demostración, que entrega sola tras la confirmación del vendedor. |
| `GLOSSUP_ENVIOS_API_KEY` | Se envía como `Authorization: Bearer`. |
| `GLOSSUP_ENVIOS_SECRETO_EVENTOS` | Secreto del webhook. Sin él se rechazan todos los eventos. |
| `GLOSSUP_ENVIOS_MINUTOS_POR_ETAPA` | Minutos por etapa de la demostración (3 por defecto). |

## Panel del vendedor
`GET /api/vendedor/ventas` (`VentasVendedorService`) devuelve los pedidos `PAGADO`/`ENVIADO` con productos del vendedor y las
cifras de `ResumenVentas` (mes en hora de Colombia, 8 semanas, más vendidos). Cada pedido trae solo sus líneas e ingresos, sin
envío ni datos de otros vendedores. Los pedidos sin pagar no se muestran: el vendedor no ve datos del cliente sin una venta real.

## Tablas (ver V1__init.sql)
`pedido` (+ `pago_transaccion_id`, `pago_metodo`, `pagado_en`, V12; + `envio_*`, V14), `pedido_item` (+ `nombre` al comprar, V13),
`comprobante` (V13), `direccion` (V14), `despacho_vendedor` (V15)
