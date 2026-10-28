import { Component, inject, input, signal } from '@angular/core';
import { CarritoService } from './carrito';

/** Botón que descarga el comprobante en PDF de un pedido pagado (HU11). */
@Component({
  selector: 'app-descargar-comprobante',
  template: `
    <button type="button" [class]="clase()" [disabled]="descargando()" (click)="descargar()">
      <span aria-hidden="true">⬇</span> {{ descargando() ? 'Preparando comprobante…' : 'Descargar comprobante (PDF)' }}
    </button>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
  `,
  styles: [`:host { display: inline-grid; gap: .35rem; justify-items: center; }`],
})
export class DescargarComprobante {
  private carrito = inject(CarritoService);
  readonly pedidoId = input.required<number>();
  readonly clase = input('btn-primario');
  readonly descargando = signal(false);
  readonly error = signal<string | null>(null);

  descargar(): void {
    this.descargando.set(true);
    this.error.set(null);
    this.carrito.comprobante(this.pedidoId()).subscribe({
      next: (r) => {
        const nombre = /filename="([^"]+)"/.exec(r.headers.get('Content-Disposition') ?? '')?.[1]
          ?? `comprobante-pedido-${this.pedidoId()}.pdf`;
        const url = URL.createObjectURL(r.body!);
        const enlace = Object.assign(document.createElement('a'), { href: url, download: nombre });
        enlace.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.descargando.set(false);
      },
      error: (e) => {
        // El cuerpo del error llega como Blob: se muestra un mensaje según el estado.
        this.error.set(e?.status === 422 ? 'Este pedido aún no tiene un pago confirmado.'
          : 'No pudimos descargar el comprobante. Inténtalo de nuevo.');
        this.descargando.set(false);
      },
    });
  }
}
