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
