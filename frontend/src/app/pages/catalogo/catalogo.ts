import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CarritoService } from '../../shared/carrito';
import type { ProductoVendedor } from '../../shared/models';

/**
 * Catálogo del cliente (HU12 - SCRUM-40): búsqueda básica de productos publicados por palabra
 * clave (nombre/marca/categoría) y alta al carrito (HU9). Acepta ?q= para llegar ya buscando
 * desde el buscador del inicio.
 */
@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Catálogo</span>
          <h1>Busca tu próximo producto.</h1>
          <p>Encuentra cosméticos publicados por nombre, marca o categoría.</p>
        </div>
      </div>

      <form class="home-search" (ngSubmit)="buscar()">
        <span aria-hidden="true">⌕</span>
        <input type="search" name="q" [(ngModel)]="q" placeholder="Busca productos, marcas o categorías" aria-label="Buscar productos" />
        <button type="submit" [disabled]="cargando()">Buscar</button>
      </form>

      @if (error()) {
        <p class="error" style="margin-top:1rem">{{ error() }}</p>
      }
      @if (aviso()) {
        <p class="success-message" style="margin-top:1rem">{{ aviso() }}</p>
      }

      <div class="section-heading">
        <div><span class="eyebrow">Resultados</span><h2>{{ titulo() }}</h2></div>
        @if (!cargando()) {
          <span class="catalog-note">{{ resultados().length }} producto(s)</span>
        }
      </div>

      @if (cargando()) {
        <p class="loading">Buscando…</p>
      } @else if (resultados().length === 0) {
        <p class="nota">No hay productos que coincidan con la búsqueda.</p>
      } @else {
        <div class="product-grid">
          @for (p of resultados(); track p.id) {
            <article class="cat-card">
              <div class="product-image">
                @if (p.imagenes?.length) {
                  <img [src]="p.imagenes[0]" [alt]="p.nombre" />
                } @else {
                  <span>✦</span>
                }
              </div>
              <h3 class="cat-nombre">{{ p.nombre }}</h3>
              <p class="cat-marca">{{ p.marca || p.categoria || 'Cosmético' }}</p>
              <div class="cat-pie">
                <span class="precio">{{ formatearCop(p.precio) }}</span>
                <button type="button" class="btn-mini" [disabled]="p.stock === 0 || agregando() === p.id" (click)="agregar(p)">
                  {{ p.stock === 0 ? 'Sin stock' : 'Agregar' }}
                </button>
              </div>
            </article>
          }
        </div>
        <p style="margin-top:1.5rem"><a routerLink="/carrito" class="perfil-callout">Ir al carrito →</a></p>
      }
    </section>
  `,
})
export class Catalogo {
  private api = inject(ProductosService);
  private carrito = inject(CarritoService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  q = '';
  readonly resultados = signal<ProductoVendedor[]>([]);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly agregando = signal<number | null>(null);
  readonly titulo = signal('Escribe para buscar');

  constructor() {
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
      this.resultados.set([]);
      this.titulo.set('Escribe para buscar');
      return;
    }
    this.cargando.set(true);
    this.titulo.set(`"${termino}"`);
    this.api.buscar(termino).subscribe({
      next: (ps) => {
        this.resultados.set(ps);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  agregar(p: ProductoVendedor): void {
    this.agregando.set(p.id);
    this.error.set(null);
    this.carrito.agregar(p.id, 1).subscribe({
      next: () => {
        this.aviso.set(`"${p.nombre}" se agregó al carrito.`);
        this.agregando.set(null);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.agregando.set(null);
      },
    });
  }
}
