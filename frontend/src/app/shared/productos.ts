import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { DatosProducto, IngredienteCatalogo, LecturaEtiqueta, ProductoVendedor, VentasVendedor } from './models';

const BASE = '/api/catalogo/productos';

/** Catálogo del vendedor. Crear/editar envían el producto completo; eliminar es borrado lógico. */
@Injectable({ providedIn: 'root' })
export class ProductosService {
  private http = inject(HttpClient);

  listarMisProductos(): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(`${BASE}/mis-productos`);
  }

  /** Catálogo publicado completo (productos activos). */
  listarCatalogo(): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(BASE);
  }

  obtener(id: number): Observable<ProductoVendedor> {
    return this.http.get<ProductoVendedor>(`${BASE}/${id}`);
  }

  /** Búsqueda básica por palabra clave en nombre/marca/categoría (HU12). */
  buscar(q: string): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(`${BASE}/buscar`, { params: { q } });
  }

  /** Busca en el catálogo cerrado de ingredientes por INCI o nombre en español. */
  buscarIngredientes(q: string): Observable<IngredienteCatalogo[]> {
    return this.http.get<IngredienteCatalogo[]>('/api/catalogo/ingredientes', { params: { q } });
  }

  crear(d: DatosProducto): Observable<ProductoVendedor> {
    return this.http.post<ProductoVendedor>(BASE, d);
  }

  actualizar(id: number, d: DatosProducto): Observable<ProductoVendedor> {
    return this.http.put<ProductoVendedor>(`${BASE}/${id}`, d);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  /** Vuelve a publicar un producto dado de baja. */
  publicar(id: number): Observable<ProductoVendedor> {
    return this.http.post<ProductoVendedor>(`${BASE}/${id}/publicar`, {});
  }

  /** Lee los ingredientes de la foto de una etiqueta (HU5). */
  leerEtiqueta(imagen: File): Observable<LecturaEtiqueta> {
    const datos = new FormData();
    datos.append('imagen', imagen);
    return this.http.post<LecturaEtiqueta>(`${BASE}/extraer-ingredientes`, datos);
  }

  /** El vendedor confirma que entregó el pedido a la transportadora. */
  marcarEnviado(pedidoId: number): Observable<void> {
    return this.http.post<void>(`/api/vendedor/ventas/${pedidoId}/enviado`, {});
  }

  /** Pedidos pagados con productos de este vendedor y sus cifras. */
  ventas(): Observable<VentasVendedor> {
    return this.http.get<VentasVendedor>('/api/vendedor/ventas');
  }
}

/** Extrae el mensaje de error que envía el backend. */
export function mensajeDeError(e: unknown): string {
  const data = (e as { error?: { message?: string; mensaje?: string; detalles?: string[] } })?.error;
  if (data?.detalles?.length) return data.detalles.join('. ');
  return data?.message ?? data?.mensaje ?? 'Ocurrió un error. Inténtalo de nuevo.';
}
