import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/auth';
import { ProductoRecomendado, ProductosService, mensajeDeError } from '../../shared/productos';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { AgregarCarrito } from '../../shared/agregar-carrito';
import type { ResultadoCompatibilidad } from '../../shared/models';

const MAX_RECOMENDADOS = 6;

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
@Component({
  selector: 'app-cliente',
  imports: [FormsModule, RouterLink, CompatInfo, AgregarCarrito],
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

      <div class="section-heading">
        <div><span class="eyebrow">Para ti</span><h2>Productos recomendados</h2></div>
        @if (!cargando()) {
          <span class="catalog-note">{{ perfilVacio() ? 'Lo más comprado y lo más nuevo' : 'Según tu perfil y tus compras' }}</span>
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
              <a class="cat-enlace" [routerLink]="['/producto', p.id]">
                <div class="product-image">
                  @if (p.imagenes?.length) {
                    <img [src]="p.imagenes[0]" [alt]="p.nombre" />
                  } @else {
                    <span>✦</span>
                  }
                </div>
                <h3 class="cat-nombre">{{ p.nombre }}</h3>
              </a>
              <p class="cat-marca">{{ p.marca || p.categoria || 'Cosmético' }}</p>
              <p class="cat-razon">{{ p.razon }}</p>
              <app-compat-info [resultado]="compat()[p.id]" />
              <div class="cat-pie">
                <span class="precio">{{ formatearCop(p.precio) }}</span>
                <button type="button" class="btn-mini" [class.agregado]="carro.agregado() === p.id"
                        [disabled]="carro.agregando() === p.id || carro.agregado() === p.id" (click)="carro.intentar(p, compat()[p.id])">
                  {{ carro.agregado() === p.id ? '✓ Agregado' : carro.agregando() === p.id ? 'Agregando…' : 'Agregar' }}
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

    <app-agregar-carrito #carro />
  `,
})
export class Cliente {
  private router = inject(Router);
  private api = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly sesion = inject(AuthService).sesion;
  readonly esqueletos = [1, 2, 3, 4, 5, 6];
  readonly recomendados = signal<ProductoRecomendado[]>([]);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
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
   * Recomendados del servidor (auditoría NC-T10): sin alertas para el perfil, ordenados por los beneficios para sus
   * condiciones, el parecido con lo que ya compró, lo que otros compraron con eso y la popularidad. Sin perfil, por
   * popularidad y novedad. Los sellos de compatibilidad se piden solo para estos productos.
   */
  private cargarRecomendados(): void {
    this.api.recomendados(MAX_RECOMENDADOS).subscribe({
      next: (r) => {
        this.perfilVacio.set(r.perfilVacio);
        this.recomendados.set(r.productos);
        this.cargando.set(false);
        if (r.productos.length) {
          this.compatibilidad.evaluar(r.productos.map((p) => p.id)).subscribe({
            next: (e) => this.compat.set(e.resultados),
            error: () => { /* sin sellos: las tarjetas igual se pueden comprar */ },
          });
        }
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
