import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';

/**
 * Estado de carga: burbujas animadas + esqueletos con brillo del tamaño del contenido real, para
 * que la página no salte al llegar los datos. Con `pasos`, el texto rota mostrando qué se está
 * haciendo. Respeta "reducir movimiento" del sistema operativo.
 */
@Component({
  selector: 'app-cargando',
  template: `
    <div class="cargando" role="status" aria-live="polite">
      <div class="cargando-cabecera">
        <span class="burbujas" aria-hidden="true"><span></span><span></span><span></span></span>
        @for (t of [actual()]; track t) { <span class="cargando-texto">{{ t }}</span> }
      </div>
      @if (forma() === 'filas') {
        <div class="esq-filas" aria-hidden="true">
          @for (i of indices(); track i) {
            <div class="esq-fila"><span class="esq esq-mini"></span><span class="esq esq-linea"></span><span class="esq esq-precio"></span></div>
          }
        </div>
      } @else {
        <div class="product-grid" aria-hidden="true">
          @for (i of indices(); track i) {
            <div class="esq-tarjeta" [style.animation-delay.ms]="i * 70">
              <span class="esq esq-img"></span>
              <span class="esq esq-linea"></span>
              <span class="esq esq-linea corta"></span>
              <span class="esq-pie"><span class="esq esq-precio"></span><span class="esq esq-boton"></span></span>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .cargando-cabecera { display: flex; align-items: center; gap: .75rem; margin: .5rem 0 1rem; color: var(--vino); font-weight: 700; }
    .cargando-texto { animation: aparecer .35s ease; }
    .burbujas { display: inline-flex; gap: .3rem; }
    .burbujas span { width: .6rem; height: .6rem; border-radius: 50%; background: var(--vino); animation: rebote 1s ease-in-out infinite; }
    .burbujas span:nth-child(2) { background: #c7839e; animation-delay: .15s; }
    .burbujas span:nth-child(3) { background: #f0b8c9; animation-delay: .3s; }

    .esq-tarjeta { display: grid; gap: .6rem; padding: .8rem; background: #fff; border: 1px solid var(--borde);
                   border-radius: 16px; animation: aparecer .4s ease both; }
    .esq { display: block; border-radius: 8px;
           background: linear-gradient(90deg, #f6eaef 0%, #fdf7f9 40%, #f6eaef 80%) 0 0 / 300% 100%;
           animation: brillo 1.4s ease-in-out infinite; }
    .esq-img { height: 155px; border-radius: 11px; }
    .esq-linea { height: .85rem; width: 85%; }
    .esq-linea.corta { width: 50%; }
    .esq-pie { display: flex; justify-content: space-between; align-items: center; margin-top: .3rem; }
    .esq-precio { height: 1rem; width: 4.5rem; }
    .esq-boton { height: 1.8rem; width: 5rem; border-radius: 999px; }

    .esq-filas { display: grid; gap: .6rem; padding: 1rem; background: #fff; border: 1px solid var(--borde); border-radius: 16px; }
    .esq-fila { display: grid; grid-template-columns: 2.5rem 1fr 5rem; gap: 1rem; align-items: center; }
    .esq-mini { height: 2.5rem; border-radius: 8px; }

    @keyframes brillo { from { background-position: 100% 0; } to { background-position: -50% 0; } }
    @keyframes rebote { 0%, 80%, 100% { transform: translateY(0) scale(.8); opacity: .6; } 40% { transform: translateY(-.45rem) scale(1); opacity: 1; } }
    @keyframes aparecer { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }

    @media (prefers-reduced-motion: reduce) {
      .esq, .burbujas span, .esq-tarjeta, .cargando-texto { animation: none; }
    }
  `],
})
export class Cargando {
  readonly texto = input('Cargando…');
  /** Mensajes que rotan mientras carga; si está vacío se muestra `texto`. */
  readonly pasos = input<string[]>([]);
  readonly cantidad = input(6);
  readonly forma = input<'tarjetas' | 'filas'>('tarjetas');
  readonly paso = signal(0);
  readonly actual = computed(() => (this.pasos().length ? this.pasos()[this.paso() % this.pasos().length] : this.texto()));

  constructor() {
    const id = setInterval(() => this.paso.update((n) => n + 1), 1400);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
  }

  indices(): number[] {
    return Array.from({ length: this.cantidad() }, (_, i) => i);
  }
}
