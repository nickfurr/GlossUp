import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/auth';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CarritoService } from '../../shared/carrito';
import { CompatibilidadService, EstadoCompatibilidad, estadoDe } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import type { ProductoVendedor, ResultadoCompatibilidad } from '../../shared/models';

const MAX_RECOMENDADOS = 6;

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
@Component({
  selector: 'app-cliente',
  imports: [FormsModule, RouterLink, CompatInfo],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Belleza que entiende tu piel</span>
          <h1>Encuentra productos para tu Glow Up, {{ sesion()?.nombre }}.</h1>
          <p>Productos cosméticos seleccionados para acompañar tu bienestar.</p>
        </div>
      </div>
      <form class="home-search" (ngSubmit)="buscar()">
        <span aria-hidden="true">⌕</span>
        <input type="search" name="q" [(ngModel)]="q" placeholder="Busca productos, marcas o ingredientes" aria-label="Buscar productos">
        <button type="submit">Buscar</button>
      </form>

      @if (error()) {
        <p class="error" style="margin-top:1rem">{{ error() }}</p>
      }
      @if (aviso()) {
        <p class="success-message" style="margin-top:1rem">{{ aviso() }}</p>
      }

      <div class="section-heading">
        <div><span class="eyebrow">Para ti</span><h2>Productos recomendados</h2></div>
        @if (!cargando()) {
          <span class="catalog-note">{{ perfilVacio() ? 'Lo más nuevo del catálogo' : 'Según tu perfil dermatológico' }}</span>
        }
      </div>

      @if (!cargando() && perfilVacio()) {
        <p class="compat-aviso">Completa tu perfil dermatológico para recibir recomendaciones compatibles con tu piel.
          <a routerLink="/perfil">Completar perfil →</a></p>
      }

      <div class="product-grid">
        @if (cargando()) {
          @for (n of esqueletos; track n) {
            <article class="product-placeholder">
              <div class="product-image"><span>✦</span></div>
              <div class="skeleton-line wide"></div><div class="skeleton-line"></div>
              <div class="skeleton-price"></div>
              <span class="placeholder-label">Cargando…</span>
            </article>
          }
        } @else {
          @for (p of recomendados(); track p.id) {
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
              <app-compat-info [resultado]="compat()[p.id]" />
              <div class="cat-pie">
                <span class="precio">{{ formatearCop(p.precio) }}</span>
                <button type="button" class="btn-mini" [disabled]="p.stock === 0 || agregando() === p.id" (click)="agregar(p)">
                  {{ p.stock === 0 ? 'Sin stock' : 'Agregar' }}
                </button>
              </div>
            </article>
          }
        }
      </div>

      @if (!cargando() && !error() && recomendados().length === 0) {
        <p class="nota">Por ahora no hay productos compatibles con tu perfil. <a routerLink="/catalogo">Explora el catálogo →</a></p>
      }
      @if (!cargando() && recomendados().length) {
        <p style="margin-top:1.5rem"><a routerLink="/catalogo" class="perfil-callout">Ver todo el catálogo →</a></p>
      }
    </section>
  `,
})
export class Cliente {
  private router = inject(Router);
  private api = inject(ProductosService);
  private carrito = inject(CarritoService);
  private compatibilidad = inject(CompatibilidadService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly sesion = inject(AuthService).sesion;
  readonly esqueletos = [1, 2, 3, 4, 5, 6];
  readonly recomendados = signal<ProductoVendedor[]>([]);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly agregando = signal<number | null>(null);
  q = '';

  constructor() {
    this.cargarRecomendados();
  }

  buscar(): void {
    this.router.navigate(['/catalogo'], { queryParams: this.q.trim() ? { q: this.q.trim() } : {} });
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  /**
   * Recomendados: catálogo publicado cruzado con el perfil (HU6). Primero los compatibles sin
   * alertas, luego los de precaución; los no compatibles se descartan. Sin perfil, lo más nuevo.
   */
  private cargarRecomendados(): void {
    this.api.listarCatalogo().subscribe({
      next: (ps) => {
        const disponibles = ps.filter((p) => p.stock > 0);
        this.compatibilidad.evaluar(disponibles.map((p) => p.id)).subscribe({
          next: (e) => {
            this.perfilVacio.set(e.perfilVacio);
            this.compat.set(e.resultados);
            this.recomendados.set(e.perfilVacio ? masNuevos(disponibles) : rankear(disponibles, e.resultados));
            this.cargando.set(false);
          },
          // Si la evaluación falla, igual mostramos productos para no dejar la portada vacía.
          error: () => {
            this.recomendados.set(masNuevos(disponibles));
            this.cargando.set(false);
          },
        });
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

function masNuevos(ps: ProductoVendedor[]): ProductoVendedor[] {
  return [...ps].sort((a, b) => (b.creadoEn ?? '').localeCompare(a.creadoEn ?? '')).slice(0, MAX_RECOMENDADOS);
}

/** Orden de recomendación; 3 = se excluye. El Record obliga a cubrir cada estado nuevo. */
const PESO: Record<EstadoCompatibilidad, number> = { recomendado: 0, compatible: 1, precaucion: 2, 'no-compatible': 3 };

function rankear(ps: ProductoVendedor[], resultados: Record<number, ResultadoCompatibilidad>): ProductoVendedor[] {
  const peso = (p: ProductoVendedor) => {
    const r = resultados[p.id];
    if (!r) return 2;
    return PESO[estadoDe(r)];
  };
  return ps
    .filter((p) => peso(p) < 3)
    .sort((a, b) => peso(a) - peso(b))
    .slice(0, MAX_RECOMENDADOS);
}
