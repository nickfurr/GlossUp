import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { Carrito, Pedido, ResultadoPago } from './models';

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

  misPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>('/api/pedidos');
  }

  obtenerPedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`/api/pedidos/${id}`);
  }

  /** URL del checkout de Wompi para pagar el pedido (HU10). */
  iniciarPago(pedidoId: number): Observable<{ url: string }> {
    return this.http.post<{ url: string }>(`/api/pedidos/${pedidoId}/pago`, {});
  }

  /** Verifica con Wompi, desde el servidor, la transacción con la que volvió el cliente. */
  confirmarPago(pedidoId: number, transaccionId: string): Observable<ResultadoPago> {
    return this.http.get<ResultadoPago>(`/api/pedidos/${pedidoId}/pago/${encodeURIComponent(transaccionId)}`);
  }

  /** Cancela un pedido sin pagar y devuelve su inventario. */
  cancelarPedido(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`/api/pedidos/${pedidoId}/cancelar`, {});
  }
}
