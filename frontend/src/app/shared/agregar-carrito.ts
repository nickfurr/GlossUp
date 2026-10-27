import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CarritoService } from './carrito';
import { CompatibilidadService } from './compatibilidad';
import { mensajeDeError } from './productos';
import type { Cruce, ProductoVendedor, ResultadoCompatibilidad } from './models';

/**
 * Agregar al carrito, igual en todas las páginas: un producto no compatible (HU6) pide confirmar el
 * riesgo; al agregar, el aviso flotante muestra los cruces (HU8) con lo que ya había en el carrito.
 * La página lo incluye una vez y llama a {@link intentar} desde sus botones.
 */
@Component({
  selector: 'app-agregar-carrito',
  imports: [RouterLink],
  template: `
    @if (aviso(); as a) {
      <div class="toast" [class.toast-alerta]="crucesNuevos().length" [class.toast-error]="a.error"
           role="status" aria-live="polite">
        <p>{{ a.error ? '✕' : '✓' }} {{ a.texto }}</p>
        @if (crucesNuevos().length) {
          <p><strong>⚠️ No conviene usarlo junto con lo que ya tienes en el carrito:</strong></p>
          <ul>
            @for (c of crucesNuevos(); track $index) { <li>{{ c.mensaje }} <span class="compat-causa">{{ c.causa }}</span></li> }
          </ul>
        }
        <div class="toast-acciones">
          <a routerLink="/carrito">Ver carrito →</a>
          <button type="button" class="btn-sec" (click)="aviso.set(null)">Cerrar</button>
        </div>
      </div>
    }

    <dialog #confirmar class="riesgo" aria-labelledby="riesgo-titulo" (close)="pendiente.set(null)">
      @if (pendiente(); as p) {
        <h2 id="riesgo-titulo">⚠️ Este producto NO es compatible contigo</h2>
        <p><strong>«{{ p.producto.nombre }}»</strong> contiene ingredientes que chocan con tu perfil dermatológico:</p>
        <ul>
          @for (a of p.compat?.alertas ?? []; track $index) {
            <li><strong>{{ a.severidad === 'ALTA' ? 'Riesgo alto' : 'Precaución' }}:</strong> {{ a.mensaje }}</li>
          }
        </ul>
        <p class="riesgo-texto">No te recomendamos comprarlo: puede causarte una reacción alérgica o irritación.
          Si decides agregarlo al carrito, lo haces <strong>bajo tu propia responsabilidad</strong>.</p>
        <div class="riesgo-acciones">
          <button type="button" class="btn-sec" autofocus (click)="confirmar.close()">No, mejor no</button>
          <button type="button" class="btn-riesgo" (click)="aceptarRiesgo()">Agregar bajo mi responsabilidad</button>
        </div>
      }
    </dialog>
  `,
  styles: [`
    .riesgo { max-width: 32rem; border: 2px solid #c8102e; border-radius: 16px; padding: 1.5rem; }
    .riesgo::backdrop { background: rgb(0 0 0 / .55); }
    .riesgo h2 { color: #9b1c36; margin-top: 0; font-size: 1.25rem; }
    .riesgo ul { padding-left: 1.1rem; display: grid; gap: .4rem; font-size: .9rem; }
    .riesgo-texto { background: #fdecef; border-radius: 10px; padding: .75rem; color: #9b1c36; }
    .riesgo-acciones { display: flex; flex-wrap: wrap; gap: .75rem; justify-content: flex-end; }
    .btn-riesgo { background: #c8102e; color: #fff; border: 0; border-radius: 999px; padding: .6rem 1.1rem; font-weight: 800; cursor: pointer; }
    .toast-error { background: #fdecef; border-color: #f2b8c4; color: #9b1c36; }
  `],
})
export class AgregarCarrito {
  private carrito = inject(CarritoService);
  private compatibilidad = inject(CompatibilidadService);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('confirmar');

  readonly agregando = signal<number | null>(null);
  readonly agregado = signal<number | null>(null);
  readonly aviso = signal<{ texto: string; error?: boolean } | null>(null);
  /** Cruces (HU8) del producto recién agregado con lo que ya había en el carrito. */
  readonly crucesNuevos = signal<Cruce[]>([]);
  readonly pendiente = signal<{ producto: ProductoVendedor; compat?: ResultadoCompatibilidad; cantidad: number; despues?: () => void } | null>(null);
  private avisoN = 0;

  /** Agrega al carrito; si el producto no es compatible, antes pide confirmar el riesgo. */
  intentar(producto: ProductoVendedor, compat?: ResultadoCompatibilidad, cantidad = 1, despues?: () => void): void {
    if (compat?.compatible === false) {
      this.pendiente.set({ producto, compat, cantidad, despues });
      this.dialogo().nativeElement.showModal();
      return;
    }
    this.agregar(producto, cantidad, `"${producto.nombre}" se agregó al carrito.`, despues);
  }

  aceptarRiesgo(): void {
    const p = this.pendiente();
    this.dialogo().nativeElement.close();
    if (p) {
      this.agregar(p.producto, p.cantidad,
        `"${p.producto.nombre}" se agregó al carrito bajo tu responsabilidad. Revisa las alertas antes de comprar.`, p.despues);
    }
  }

  private agregar(p: ProductoVendedor, cantidad: number, mensaje: string, despues?: () => void): void {
    this.agregando.set(p.id);
    this.carrito.agregar(p.id, cantidad).subscribe({
      next: (carrito) => {
        this.agregando.set(null);
        this.agregado.set(p.id);
        setTimeout(() => this.agregado.update((id) => (id === p.id ? null : id)), 2000);
        if (despues) {
          despues();
          return;
        }
        this.mostrar({ texto: mensaje });
        this.crucesNuevos.set([]);
        this.compatibilidad.cruces(carrito.items.map((i) => i.productoId)).subscribe({
          next: (cs) => this.crucesNuevos.set(cs.filter((c) => c.productoA === p.id || c.productoB === p.id)),
          error: () => {},
        });
      },
      error: (e) => {
        this.agregando.set(null);
        this.crucesNuevos.set([]);
        this.mostrar({ texto: mensajeDeError(e), error: true });
      },
    });
  }

  /** El aviso simple se cierra solo; con cruces o error se queda hasta que el cliente lo cierre. */
  private mostrar(a: { texto: string; error?: boolean }): void {
    this.aviso.set(a);
    const n = ++this.avisoN;
    setTimeout(() => {
      if (n === this.avisoN && !a.error && !this.crucesNuevos().length) this.aviso.set(null);
    }, 5000);
  }
}
