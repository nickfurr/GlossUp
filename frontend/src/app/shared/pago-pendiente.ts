import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { CarritoService } from './carrito';
import { mensajeDeError } from './productos';
import type { Pedido } from './models';

/**
 * Pedido CREADO esperando pago (HU10): plazo restante, pagar con Wompi o cancelar (devuelve el
 * inventario). Pagar redirige al checkout de Wompi; la tarjeta nunca pasa por GlossUP.
 */
@Component({
  selector: 'app-pago-pendiente',
  template: `
    <div class="panel pendiente" role="region" [attr.aria-label]="'Pedido ' + pedido().id + ' pendiente de pago'">
      <div class="pendiente-info">
        <span class="eyebrow">Pendiente de pago</span>
        <h3>Pedido #{{ pedido().id }} · {{ cop.format(pedido().total) }}</h3>
        @if (minutosRestantes() > 0) {
          <p>Reservamos tu inventario <strong>{{ minutosRestantes() }} min</strong> más. Si no pagas, se libera para otros clientes.</p>
        } @else {
          <p>El plazo de pago terminó; en unos minutos el inventario vuelve al catálogo.</p>
        }
      </div>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <div class="pendiente-acciones">
        @if (confirmandoCancelacion()) {
          <span>¿Cancelar el pedido #{{ pedido().id }}?</span>
          <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="confirmandoCancelacion.set(false)">No</button>
          <button type="button" class="btn-sec peligro" [disabled]="ocupado()" (click)="cancelar()">Sí, cancelar</button>
        } @else {
          <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="confirmandoCancelacion.set(true)">Cancelar pedido</button>
          <button type="button" class="btn-primario" [disabled]="ocupado() || minutosRestantes() <= 0" (click)="pagar()">
            {{ ocupado() ? 'Conectando con Wompi…' : 'Pagar con Wompi' }}
          </button>
        }
      </div>
      <p class="seguro">🔒 Pagas en la página segura de Wompi (tarjeta, PSE, Nequi o Bancolombia). GlossUP nunca ve los datos de tu tarjeta.</p>
    </div>
  `,
  styles: [`
    .pendiente { display: grid; gap: .6rem; border: 2px solid #f2dca6; background: #fffaf0; margin-bottom: 1rem; }
    .pendiente h3 { margin: .1rem 0; }
    .pendiente p { margin: 0; }
    .pendiente-acciones { display: flex; flex-wrap: wrap; gap: .6rem; align-items: center; justify-content: flex-end; }
    .peligro { color: #9b1c36; border-color: #f2b8c4; }
    .seguro { font-size: .8rem; color: #6b5a61; }
  `],
})
export class PagoPendiente {
  private carrito = inject(CarritoService);
  readonly pedido = input.required<Pedido>();
  readonly cancelado = output<Pedido>();
  readonly cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly confirmandoCancelacion = signal(false);
  private readonly ahora = signal(Date.now());
  readonly minutosRestantes = computed(() => {
    const expira = this.pedido().expiraEn;
    return expira ? Math.max(0, Math.ceil((Date.parse(expira) - this.ahora()) / 60_000)) : 0;
  });

  constructor() {
    const id = setInterval(() => this.ahora.set(Date.now()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
  }

  pagar(): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.carrito.iniciarPago(this.pedido().id).subscribe({
      next: ({ url }) => window.location.assign(url),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  cancelar(): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.carrito.cancelarPedido(this.pedido().id).subscribe({
      next: (p) => this.cancelado.emit(p),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
        this.confirmandoCancelacion.set(false);
      },
    });
  }
}
