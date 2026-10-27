import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CarritoService } from '../../shared/carrito';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import type { Cruce, ProductoVendedor, ResultadoCompatibilidad } from '../../shared/models';

/**
 * Catálogo del cliente, estilo marketplace: carrusel "Recomendados para ti" (HU6: productos sin
 * alertas y con ingredientes buenos para su perfil), filtro por categoría, búsqueda (HU12) y alta
 * al carrito (HU9). Un producto no compatible se puede agregar, pero solo tras confirmar el riesgo.
 */
@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, RouterLink, CompatInfo, NgTemplateOutlet],
  template: `
    <section class="cliente-home">
      <form class="home-search" (ngSubmit)="buscar()">
        <span aria-hidden="true">⌕</span>
        <input type="search" name="q" [(ngModel)]="q" placeholder="Busca productos, marcas o categorías" aria-label="Buscar productos" />
        <button type="submit" [disabled]="cargando()">Buscar</button>
      </form>

      @if (error()) { <p class="error" style="margin-top:1rem">{{ error() }}</p> }

      @if (!busqueda()) {
        <div class="section-heading">
          <div><span class="eyebrow">Según tu perfil dermatológico</span><h2>Recomendados para ti</h2></div>
          @if (recomendados().length) { <span class="catalog-note">{{ recomendados().length }} producto(s)</span> }
        </div>
        @if (perfilVacio()) {
          <p class="compat-aviso">Completa tu perfil dermatológico y te recomendaremos los productos que le sirven a tu piel y cabello.
            <a routerLink="/perfil">Completar mi perfil →</a></p>
        } @else if (recomendados().length === 0 && !cargando()) {
          <p class="nota">Aún no tenemos productos con ingredientes específicos para tu perfil.</p>
        } @else {
          <div class="carrusel" role="list" aria-label="Productos recomendados para ti">
            @for (p of recomendados(); track p.id) {
              <ng-container *ngTemplateOutlet="tarjeta; context: { $implicit: p }" />
            }
          </div>
        }
      }

      <div class="section-heading">
        <div><span class="eyebrow">{{ busqueda() ? 'Resultados' : 'Catálogo' }}</span><h2>{{ busqueda() ? '"' + busqueda() + '"' : 'Todos los productos' }}</h2></div>
        @if (busqueda()) {
          <button type="button" class="btn-sec" (click)="limpiarBusqueda()">✕ Quitar búsqueda</button>
        } @else if (!cargando()) {
          <span class="catalog-note">{{ visibles().length }} producto(s)</span>
        }
      </div>

      @if (!busqueda() && categorias().length > 1) {
        <div class="chips" role="group" aria-label="Filtrar por categoría">
          @for (c of categorias(); track c) {
            <button type="button" class="btn-sec" [class.activo]="categoria() === c" [attr.aria-pressed]="categoria() === c"
                    (click)="categoria.set(c)">{{ c }}</button>
          }
        </div>
      }

      @if (cargando()) {
        <p class="loading">Cargando productos…</p>
      } @else if (visibles().length === 0) {
        <p class="nota">No hay productos que coincidan.</p>
      } @else {
        <div class="product-grid">
          @for (p of visibles(); track p.id) {
            <ng-container *ngTemplateOutlet="tarjeta; context: { $implicit: p }" />
          }
        </div>
        <p style="margin-top:1.5rem"><a routerLink="/carrito" class="perfil-callout">Ir al carrito →</a></p>
      }
    </section>

    <ng-template #tarjeta let-p>
      <article class="cat-card" role="listitem">
        <div class="product-image">
          @if (p.imagenes?.length) { <img [src]="p.imagenes[0]" [alt]="p.nombre" loading="lazy" /> } @else { <span>✦</span> }
        </div>
        <h3 class="cat-nombre">{{ p.nombre }}</h3>
        <p class="cat-marca">{{ p.marca || p.categoria || 'Cosmético' }}</p>
        <app-compat-info [resultado]="compat()[p.id]" />
        <div class="cat-pie">
          <span class="precio">{{ formatearCop(p.precio) }}</span>
          <button type="button" class="btn-mini" [disabled]="p.stock === 0 || agregando() === p.id" (click)="intentarAgregar(p)">
            {{ p.stock === 0 ? 'Sin stock' : 'Agregar' }}
          </button>
        </div>
      </article>
    </ng-template>

    @if (aviso()) {
      <div class="toast" [class.toast-alerta]="crucesNuevos().length" role="status" aria-live="polite">
        <p>{{ aviso() }}</p>
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
        <p><strong>«{{ p.nombre }}»</strong> contiene ingredientes que chocan con tu perfil dermatológico:</p>
        <ul>
          @for (a of compat()[p.id]?.alertas ?? []; track $index) {
            <li><strong>{{ a.severidad === 'ALTA' ? 'Riesgo alto' : 'Precaución' }}:</strong> {{ a.mensaje }}</li>
          }
        </ul>
        <p class="riesgo-texto">No te recomendamos comprarlo: puede causarte una reacción alérgica o irritación.
          Si decides agregarlo al carrito, lo haces <strong>bajo tu propia responsabilidad</strong>.</p>
        <div class="riesgo-acciones">
          <button type="button" class="btn-sec" autofocus (click)="confirmar.close()">No, mejor no</button>
          <button type="button" class="btn-riesgo" (click)="aceptarRiesgo(p)">Agregar bajo mi responsabilidad</button>
        </div>
      }
    </dialog>
  `,
  styles: [`
    .carrusel { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(210px, 240px); gap: 1rem;
                overflow-x: auto; scroll-snap-type: x mandatory; padding-bottom: .75rem; }
    .carrusel .cat-card { scroll-snap-align: start; }
    .chips { display: flex; flex-wrap: wrap; gap: .5rem; margin-bottom: 1rem; }
    .chips .activo { background: var(--vino); color: #fff; border-color: var(--vino); }
    .riesgo { max-width: 32rem; border: 2px solid #c8102e; border-radius: 16px; padding: 1.5rem; }
    .riesgo::backdrop { background: rgb(0 0 0 / .55); }
    .riesgo h2 { color: #9b1c36; margin-top: 0; font-size: 1.25rem; }
    .riesgo ul { padding-left: 1.1rem; display: grid; gap: .4rem; font-size: .9rem; }
    .riesgo-texto { background: #fdecef; border-radius: 10px; padding: .75rem; color: #9b1c36; }
    .riesgo-acciones { display: flex; flex-wrap: wrap; gap: .75rem; justify-content: flex-end; }
    .toast { position: fixed; bottom: 1rem; left: 50%; transform: translateX(-50%); z-index: 50;
             width: min(32rem, calc(100% - 2rem)); padding: .9rem 1rem; border-radius: 14px; border: 1px solid #b9dfc5;
             background: #eaf6ee; color: #1f6b3a; box-shadow: 0 10px 30px rgb(0 0 0 / .18); }
    .toast p { margin: 0 0 .4rem; }
    .toast ul { margin: 0 0 .5rem; padding-left: 1.1rem; display: grid; gap: .35rem; font-size: .9rem; }
    .toast-alerta { background: #fdf4e1; border-color: #f2dca6; color: #8a5d00; }
    .toast-acciones { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .toast-acciones a { color: var(--vino); font-weight: 800; }
    .btn-riesgo { background: #c8102e; color: #fff; border: 0; border-radius: 999px; padding: .6rem 1.1rem; font-weight: 800; cursor: pointer; }
  `],
})
export class Catalogo {
  private api = inject(ProductosService);
  private carrito = inject(CarritoService);
  private compatibilidad = inject(CompatibilidadService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  readonly confirmar = viewChild.required<ElementRef<HTMLDialogElement>>('confirmar');

  q = '';
  readonly catalogo = signal<ProductoVendedor[]>([]);
  readonly resultados = signal<ProductoVendedor[]>([]);
  readonly busqueda = signal('');
  readonly categoria = signal('Todas');
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  /** Cruces (HU8) del producto recién agregado con lo que ya había en el carrito. */
  readonly crucesNuevos = signal<Cruce[]>([]);
  private avisoN = 0;
  readonly agregando = signal<number | null>(null);
  readonly pendiente = signal<ProductoVendedor | null>(null);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);

  /** Los que más beneficios aportan al perfil, primero. */
  readonly recomendados = computed(() =>
    this.catalogo()
      .filter((p) => this.compat()[p.id]?.recomendado)
      .sort((a, b) => this.compat()[b.id].beneficios.length - this.compat()[a.id].beneficios.length),
  );
  readonly categorias = computed(() => ['Todas', ...new Set(this.catalogo().map((p) => p.categoria).filter((c): c is string => !!c))]);
  readonly visibles = computed(() => {
    if (this.busqueda()) return this.resultados();
    const c = this.categoria();
    return c === 'Todas' ? this.catalogo() : this.catalogo().filter((p) => p.categoria === c);
  });

  constructor() {
    this.api.listarCatalogo().subscribe({
      next: (ps) => {
        this.catalogo.set(ps);
        this.cargando.set(false);
        this.evaluarCompatibilidad(ps);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
    const q = inject(ActivatedRoute).snapshot.queryParamMap.get('q');
    if (q) {
      this.q = q;
      this.buscar();
    }
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  buscar(): void {
    const termino = this.q.trim();
    this.error.set(null);
    this.aviso.set(null);
    if (!termino) {
      this.limpiarBusqueda();
      return;
    }
    this.cargando.set(true);
    this.busqueda.set(termino);
    this.api.buscar(termino).subscribe({
      next: (ps) => {
        this.resultados.set(ps);
        this.cargando.set(false);
        this.evaluarCompatibilidad(ps.filter((p) => !this.compat()[p.id]));
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  limpiarBusqueda(): void {
    this.q = '';
    this.busqueda.set('');
    this.resultados.set([]);
  }

  /** Una sola petición por lote; se acumula en el mapa. Si falla, el catálogo sigue usable sin sellos. */
  private evaluarCompatibilidad(ps: ProductoVendedor[]): void {
    if (!ps.length) return;
    this.compatibilidad.evaluar(ps.map((p) => p.id)).subscribe({
      next: (e) => {
        this.perfilVacio.set(e.perfilVacio);
        this.compat.update((c) => ({ ...c, ...e.resultados }));
      },
      error: () => {},
    });
  }

  /** Un producto no compatible exige confirmar el riesgo antes de ir al carrito. */
  intentarAgregar(p: ProductoVendedor): void {
    if (this.compat()[p.id]?.compatible === false) {
      this.pendiente.set(p);
      this.confirmar().nativeElement.showModal();
      return;
    }
    this.agregar(p, `"${p.nombre}" se agregó al carrito.`);
  }

  aceptarRiesgo(p: ProductoVendedor): void {
    this.confirmar().nativeElement.close();
    this.agregar(p, `"${p.nombre}" se agregó al carrito bajo tu responsabilidad. Revisa las alertas antes de comprar.`);
  }

  private agregar(p: ProductoVendedor, mensaje: string): void {
    this.agregando.set(p.id);
    this.error.set(null);
    this.carrito.agregar(p.id, 1).subscribe({
      next: (carrito) => {
        this.aviso.set(mensaje);
        this.crucesNuevos.set([]);
        this.agregando.set(null);
        this.compatibilidad.cruces(carrito.items.map((i) => i.productoId)).subscribe({
          next: (cs) => this.crucesNuevos.set(cs.filter((c) => c.productoA === p.id || c.productoB === p.id)),
          error: () => {},
        });
        // El aviso simple se cierra solo; si hay cruces se queda hasta que el cliente lo cierre.
        const n = ++this.avisoN;
        setTimeout(() => { if (n === this.avisoN && !this.crucesNuevos().length) this.aviso.set(null); }, 5000);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.agregando.set(null);
      },
    });
  }
}
