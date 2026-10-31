import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ProductosService } from './productos';
import type { Carrito, LineaVista, Pedido, ResultadoPago } from './models';

/**
 * Carrito del cliente (HU9). Agregar/cambiar/quitar/vaciar devuelven el carrito actualizado; el
 * checkout (`consolidar`) reserva inventario de forma atómica y crea el pedido.
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private http = inject(HttpClient);
  private productos = inject(ProductosService);

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

  /** Completa cada línea con nombre e imagen del producto; si una consulta falla, usa "Producto #id". */
  detallar(c: Carrito): Observable<LineaVista[]> {
    if (c.items.length === 0) return of([]);
    return forkJoin(
      c.items.map((it) =>
        this.productos.obtener(it.productoId).pipe(
          map((p): LineaVista => ({ ...it, nombre: p.nombre, imagen: p.imagenes?.[0] })),
          catchError(() => of<LineaVista>({ ...it, nombre: `Producto #${it.productoId}` })),
        ),
      ),
    );
  }

  /** Consolida el carrito, reserva inventario y fija la entrega. Falla sin reservar nada si falta stock. */
  consolidar(items: { productoId: number; cantidad: number }[], direccionId: number, metodoEnvio: string): Observable<Pedido> {
    return this.http.post<Pedido>('/api/pedidos', { items, direccionId, metodoEnvio });
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

  /** PDF del comprobante de compra de un pedido pagado (HU11). */
  comprobante(pedidoId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/pedidos/${pedidoId}/comprobante`, { responseType: 'blob', observe: 'response' });
  }
}
