import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { Cargando } from '../../shared/cargando';
import { AgregarCarrito } from '../../shared/agregar-carrito';
import type { ProductoVendedor, ResultadoCompatibilidad } from '../../shared/models';

/**
 * Catálogo del cliente, estilo marketplace: carrusel "Recomendados para ti" (HU6: productos sin
 * alertas y con ingredientes buenos para su perfil), filtro por categoría, búsqueda (HU12) y alta
 * al carrito (HU9). Cada tarjeta abre la ficha del producto.
 */
@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, RouterLink, CompatInfo, NgTemplateOutlet, Cargando, AgregarCarrito],
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
        @if (cargando() || evaluando()) {
          <app-cargando [pasos]="pasosRecomendados" [cantidad]="3" />
        } @else if (perfilVacio()) {
          <p class="compat-aviso">Completa tu perfil dermatológico y te recomendaremos los productos que le sirven a tu piel y cabello.
            <a routerLink="/perfil">Completar mi perfil →</a></p>
        } @else if (recomendados().length === 0) {
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
        <app-cargando [texto]="busqueda() ? 'Buscando productos…' : 'Cargando productos…'" />
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
        <a class="cat-enlace" [routerLink]="['/producto', p.id]">
          <div class="product-image">
            @if (p.imagenes?.length) { <img [src]="p.imagenes[0]" [alt]="p.nombre" loading="lazy" /> } @else { <span>✦</span> }
          </div>
          <h3 class="cat-nombre">{{ p.nombre }}</h3>
        </a>
        <p class="cat-marca">{{ p.marca || p.categoria || 'Cosmético' }}</p>
        <app-compat-info [resultado]="compat()[p.id]" />
        <div class="cat-pie">
          <span class="precio">{{ formatearCop(p.precio) }}</span>
          <button type="button" class="btn-mini" [class.agregado]="carro.agregado() === p.id"
                  [disabled]="carro.agregando() === p.id || carro.agregado() === p.id" (click)="carro.intentar(p, compat()[p.id])">
            {{ carro.agregado() === p.id ? '✓ Agregado' : carro.agregando() === p.id ? 'Agregando…' : 'Agregar' }}
          </button>
        </div>
      </article>
    </ng-template>

    <app-agregar-carrito #carro />
  `,
  styles: [`
    .carrusel { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(210px, 240px); gap: 1rem;
                overflow-x: auto; scroll-snap-type: x mandatory; padding-bottom: .75rem; }
    .carrusel .cat-card { scroll-snap-align: start; }
    .chips { display: flex; flex-wrap: wrap; gap: .5rem; margin-bottom: 1rem; }
    .chips .activo { background: var(--vino); color: #fff; border-color: var(--vino); }
  `],
})
export class Catalogo {
  private api = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  q = '';
  readonly catalogo = signal<ProductoVendedor[]>([]);
  readonly resultados = signal<ProductoVendedor[]>([]);
  readonly busqueda = signal('');
  readonly categoria = signal('Todas');
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);
  /** La primera evaluación contra el perfil, de la que salen los recomendados. */
  readonly evaluando = signal(true);
  readonly pasosRecomendados = ['Buscando lo mejor para tu piel…', 'Revisando tus afecciones y tipo de piel…', 'Eligiendo tus recomendados…'];

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
        this.evaluando.set(false);
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
    if (!ps.length) { this.evaluando.set(false); return; }
    this.compatibilidad.evaluar(ps.map((p) => p.id)).subscribe({
      next: (e) => {
        this.perfilVacio.set(e.perfilVacio);
        this.compat.update((c) => ({ ...c, ...e.resultados }));
        this.evaluando.set(false);
      },
      error: () => this.evaluando.set(false),
    });
  }
}
