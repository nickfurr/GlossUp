// Tipos de dominio compartidos, alineados con la API del backend.

export type RolUsuario = 'CLIENTE' | 'VENDEDOR' | 'ADMIN';
export type Panel = RolUsuario;

export interface SesionUsuario {
  usuarioId: number;
  nombre: string;
  email: string;
  roles: RolUsuario[];
  panel: Panel;
}

/** Ingrediente de la ficha INCI, tal como lo expone el backend. */
export interface IngredienteFicha {
  id?: number;
  inci: string;
  descripcion?: string | null;
  concentracion?: number | null;
}

/** Producto del catálogo (GET /mis-productos, /{id}). */
export interface ProductoVendedor {
  id: number;
  vendedorId: number;
  nombre: string;
  marca?: string | null;
  categoria?: string | null;
  precio: number;
  stock: number;
  ph?: number | null;
  registroInvima?: string | null;
  /** Descripción comercial que redacta el vendedor. */
  descripcion?: string | null;
  activo: boolean;
  imagenes: string[];
  ingredientes: IngredienteFicha[];
  creadoEn?: string;
}

/** Ingrediente del catálogo cerrado (GET /api/catalogo/ingredientes?q=). */
export interface IngredienteCatalogo {
  id: number;
  inci: string;
  /** Nombre en español y función, ej. "Glicerina · humectante". */
  descripcion: string | null;
}

/** Cuerpo para registrar/actualizar un producto. La ficha INCI es obligatoria (≥ 1) y solo admite INCI del catálogo. */
export interface DatosProducto {
  nombre: string;
  marca?: string;
  categoria?: string;
  precio: number;
  stock: number;
  ph?: number | null;
  registroInvima?: string;
  descripcion?: string;
  imagenes: string[];
  ingredientes: { inci: string; concentracion?: number | null }[];
}

/** Línea del carrito/pedido tal como la expone el backend (HU9). */
export interface LineaCompra {
  productoId: number;
  /** Solo en pedidos: nombre del producto al momento de la compra (HU11). */
  nombre?: string | null;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

/** Carrito del cliente (GET /api/carrito). */
export interface Carrito {
  usuarioId: number;
  items: LineaCompra[];
  total: number;
}

/** Línea del carrito con nombre e imagen del producto, para mostrarla. */
export interface LineaVista extends LineaCompra {
  nombre: string;
  imagen?: string;
}

/** Dirección de la libreta del cliente (HU14). */
export interface Direccion {
  id: number;
  nombreRecibe: string;
  telefono: string;
  departamento: string;
  ciudad: string;
  barrio: string;
  direccion: string;
  complemento?: string | null;
  indicaciones?: string | null;
  principal: boolean;
}

export type DatosDireccion = Omit<Direccion, 'id'>;

/** Forma de envío disponible para un destino (GET /api/envios/opciones). */
export interface OpcionEnvio {
  metodo: string;
  etiqueta: string;
  costo: number;
  fechaEstimada: string;
}

export type EstadoEnvio = 'PREPARANDO' | 'LISTO_PARA_ENVIO' | 'EN_CAMINO' | 'ENTREGADO' | 'NO_ENTREGADO';

/** Envío de un pedido: copia de la dirección al comprar y seguimiento (HU14). */
export interface EnvioPedido {
  nombreRecibe: string;
  telefono: string;
  departamento: string;
  ciudad: string;
  barrio: string;
  direccion: string;
  complemento?: string | null;
  indicaciones?: string | null;
  metodo: string;
  metodoEtiqueta: string;
  costo: number;
  fechaEstimada: string;
  estado: EstadoEnvio;
  transportadora?: string | null;
  guia?: string | null;
  notificadoEn?: string | null;
  enviadoEn?: string | null;
  entregadoEn?: string | null;
  noEntregadoEn?: string | null;
}

/** Pedido pagado visto por el vendedor: solo sus líneas e ingresos, sin el envío (GET /api/vendedor/ventas). */
export interface VentaVendedor {
  id: number;
  estado: string;
  cliente: string;
  pagadoEn: string;
  items: LineaCompra[];
  ingresos: number;
  envio?: EnvioPedido | null;
}

export interface ResumenVentas {
  ingresosMes: number;
  pedidosMes: number;
  unidadesMes: number;
  semanas: { inicio: string; ingresos: number }[];
  masVendidos: { productoId: number; nombre: string; unidades: number; ingresos: number }[];
}

export interface VentasVendedor {
  resumen: ResumenVentas;
  ventas: VentaVendedor[];
}

/** Lo que devuelve leer una etiqueta: todo lo detectado y lo que se puede agregar a la ficha (HU5). */
export interface LecturaEtiqueta {
  ingredientes: string[];
  enCatalogo: IngredienteCatalogo[];
}

/** Pedido consolidado con inventario reservado (POST /api/pedidos). */
export interface Pedido {
  id: number;
  usuarioId: number;
  estado: string;
  items: LineaCompra[];
  subtotal: number;
  /** Null solo en pedidos anteriores a HU14. */
  envio?: EnvioPedido | null;
  total: number;
  creadoEn?: string;
  /** Solo en pedidos CREADO: hasta cuándo se puede pagar antes de que se libere el inventario. */
  expiraEn?: string | null;
  transaccionId?: string | null;
  metodoPago?: string | null;
  pagadoEn?: string | null;
}

export type EstadoTransaccion = 'APROBADA' | 'PENDIENTE' | 'RECHAZADA' | 'ANULADA' | 'ERROR';

/** Resultado de verificar un pago con Wompi (HU10). */
export interface ResultadoPago {
  pedido: Pedido;
  estadoTransaccion: EstadoTransaccion;
}

export type TipoPiel = 'NORMAL' | 'SECA' | 'GRASA' | 'MIXTA' | 'SENSIBLE';

/** Opción de un catálogo cerrado del perfil. Se envía siempre `codigo`; lo demás es para mostrar. */
export interface OpcionPerfil {
  codigo: string;
  etiqueta: string;
  /** Descripción del alérgeno, o zona de la afección (PIEL, CABELLO, GENERAL). */
  detalle: string | null;
}

/** Catálogos cerrados del perfil (GET /api/perfil/me/opciones). */
export interface OpcionesPerfil {
  tiposPiel: OpcionPerfil[];
  tiposCabello: OpcionPerfil[];
  alergias: OpcionPerfil[];
  afecciones: OpcionPerfil[];
}

export interface PerfilDermatologico {
  id?: number | null;
  usuarioId: number;
  ph: number | null;
  tipoPiel: TipoPiel | null;
  tipoCabello: string | null;
  /** Códigos de `Afeccion` (ej. ROSACEA). */
  afecciones: string[];
  /** Códigos de `Alergeno` (ej. PARABENOS). */
  alergias: string[];
  actualizadoEn: string;
}

/** Alerta del motor de compatibilidad (HU6): siempre trae su causa (RNF-05). */
export interface AlertaCompatibilidad {
  severidad: 'ALTA' | 'MEDIA' | 'BAJA';
  mensaje: string;
  causa: string;
}

/** Choque entre dos productos del carrito (HU8): no conviene usarlos al mismo tiempo. */
export interface Cruce {
  productoA: number;
  productoB: number;
  severidad: 'ALTA' | 'MEDIA' | 'BAJA';
  mensaje: string;
  causa: string;
}

/** Resultado de evaluar un producto contra el perfil. `compatible` es false si hay alguna alerta ALTA. */
export interface ResultadoCompatibilidad {
  productoId: number;
  compatible: boolean;
  /** Sin ninguna alerta y con ingredientes recomendados para alguna condición del perfil. */
  recomendado: boolean;
  alertas: AlertaCompatibilidad[];
  /** Por qué le conviene al perfil: un texto por ingrediente recomendado. */
  beneficios: string[];
}
