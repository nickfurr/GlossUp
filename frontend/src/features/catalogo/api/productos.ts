import { apiClient } from '../../../shared/api/apiClient'

/** Ingrediente de la ficha INCI, tal como lo expone el backend (nomenclatura de nick). */
export interface IngredienteFicha {
  id?: number
  inci: string
  descripcion?: string | null
  concentracion?: number | null
}

/** Producto tal como lo devuelve la API del catálogo (GET /mis-productos, /{id}). */
export interface ProductoVendedor {
  id: number
  vendedorId: number
  nombre: string
  marca?: string | null
  categoria?: string | null
  precio: number
  stock: number
  ph?: number | null
  registroInvima?: string | null
  activo: boolean
  imagenes: string[]
  ingredientes: IngredienteFicha[]
  creadoEn?: string
}

/** Cuerpo para registrar/actualizar un producto. La ficha INCI es obligatoria (≥ 1). */
export interface DatosProducto {
  nombre: string
  marca?: string
  categoria?: string
  precio: number
  stock: number
  ph?: number | null
  registroInvima?: string
  imagenes: string[]
  ingredientes: { inci: string; descripcion?: string | null; concentracion?: number | null }[]
}

const BASE = '/catalogo/productos'

export async function listarMisProductos(): Promise<ProductoVendedor[]> {
  const { data } = await apiClient.get<ProductoVendedor[]>(`${BASE}/mis-productos`)
  return data
}

export async function obtenerProducto(id: number): Promise<ProductoVendedor> {
  const { data } = await apiClient.get<ProductoVendedor>(`${BASE}/${id}`)
  return data
}

export async function crearProducto(d: DatosProducto): Promise<ProductoVendedor> {
  const { data } = await apiClient.post<ProductoVendedor>(BASE, d)
  return data
}

export async function actualizarProducto(id: number, d: DatosProducto): Promise<ProductoVendedor> {
  const { data } = await apiClient.put<ProductoVendedor>(`${BASE}/${id}`, d)
  return data
}

/** Borrado lógico (el backend pone activo = false y conserva el histórico). */
export async function eliminarProducto(id: number): Promise<void> {
  await apiClient.delete(`${BASE}/${id}`)
}

/** Extrae el mensaje de error que envía el backend. */
export function mensajeDeError(e: unknown): string {
  const resp = (e as { response?: { data?: { message?: string; mensaje?: string; detalles?: string[] } } })?.response
    ?.data
  if (resp?.detalles?.length) return resp.detalles.join('. ')
  return resp?.message ?? resp?.mensaje ?? 'Ocurrió un error. Inténtalo de nuevo.'
}
