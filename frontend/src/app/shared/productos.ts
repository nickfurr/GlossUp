import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { DatosProducto, ProductoVendedor } from './models';

const BASE = '/api/catalogo/productos';

/** Catálogo del vendedor. Crear/editar envían el producto completo; eliminar es borrado lógico. */
@Injectable({ providedIn: 'root' })
export class ProductosService {
  private http = inject(HttpClient);

  listarMisProductos(): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(`${BASE}/mis-productos`);
  }

  obtener(id: number): Observable<ProductoVendedor> {
    return this.http.get<ProductoVendedor>(`${BASE}/${id}`);
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
}

/** Extrae el mensaje de error que envía el backend. */
export function mensajeDeError(e: unknown): string {
  const data = (e as { error?: { message?: string; mensaje?: string; detalles?: string[] } })?.error;
  if (data?.detalles?.length) return data.detalles.join('. ');
  return data?.message ?? data?.mensaje ?? 'Ocurrió un error. Inténtalo de nuevo.';
}
