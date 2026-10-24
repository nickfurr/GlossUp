import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Carrito, Pedido } from './models';

/**
 * Carrito del cliente (HU9). Agregar/cambiar/quitar/vaciar devuelven el carrito actualizado; el
 * checkout (`consolidar`) reserva inventario de forma atómica y crea el pedido.
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private http = inject(HttpClient);

  ver(): Observable<Carrito> {
    return this.http.get<Carrito>('/api/carrito');
  }

  agregar(productoId: number, cantidad: number): Observable<Carrito> {
    return this.http.post<Carrito>('/api/carrito/items', { productoId, cantidad });
  }

  cambiarCantidad(productoId: number, cantidad: number): Observable<Carrito> {
    return this.http.put<Carrito>(`/api/carrito/items/${productoId}`, { cantidad });
  }

  quitar(productoId: number): Observable<Carrito> {
    return this.http.delete<Carrito>(`/api/carrito/items/${productoId}`);
  }

  vaciar(): Observable<Carrito> {
    return this.http.delete<Carrito>('/api/carrito');
  }

  /** Consolida el carrito y reserva inventario. Falla sin reservar nada si falta stock. */
  consolidar(items: { productoId: number; cantidad: number }[]): Observable<Pedido> {
    return this.http.post<Pedido>('/api/pedidos', { items });
  }
}
