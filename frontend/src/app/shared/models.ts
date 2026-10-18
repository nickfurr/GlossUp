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
  activo: boolean;
  imagenes: string[];
  ingredientes: IngredienteFicha[];
  creadoEn?: string;
}

/** Cuerpo para registrar/actualizar un producto. La ficha INCI es obligatoria (≥ 1). */
export interface DatosProducto {
  nombre: string;
  marca?: string;
  categoria?: string;
  precio: number;
  stock: number;
  ph?: number | null;
  registroInvima?: string;
  imagenes: string[];
  ingredientes: { inci: string; descripcion?: string | null; concentracion?: number | null }[];
}

export type TipoPiel = 'NORMAL' | 'SECA' | 'GRASA' | 'MIXTA' | 'SENSIBLE';
export type TipoCabello =
  | 'LISO_1A' | 'LISO_1B' | 'LISO_1C'
  | 'ONDULADO_2A' | 'ONDULADO_2B' | 'ONDULADO_2C'
  | 'RIZADO_3A' | 'RIZADO_3B' | 'RIZADO_3C'
  | 'MUY_RIZADO_4A' | 'MUY_RIZADO_4B' | 'MUY_RIZADO_4C';

export interface PerfilDermatologico {
  id?: number | null;
  usuarioId: number;
  ph: number | null;
  tipoPiel: TipoPiel | null;
  tipoCabello: TipoCabello | null;
  afecciones: string[];
  alergias: string[];
  actualizadoEn: string;
}
