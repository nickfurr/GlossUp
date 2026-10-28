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

/** Pedido consolidado con inventario reservado (POST /api/pedidos). */
export interface Pedido {
  id: number;
  usuarioId: number;
  estado: string;
  items: LineaCompra[];
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
