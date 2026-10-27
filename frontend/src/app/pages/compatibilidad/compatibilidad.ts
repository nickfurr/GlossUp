import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { CompatibilidadService, EstadoCompatibilidad, estadoDe } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { Cargando } from '../../shared/cargando';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import type { ProductoVendedor, ResultadoCompatibilidad } from '../../shared/models';

type Filtro = 'todos' | EstadoCompatibilidad;
const ORDEN: Record<EstadoCompatibilidad, number> = { 'no-compatible': 0, precaucion: 1, compatible: 2, recomendado: 3 };

/**
 * Compatibilidad producto–perfil (HU6): todo el catálogo publicado evaluado contra el perfil
 * dermatológico del cliente, agrupado en no compatibles, con precaución y compatibles.
 */
@Component({
  selector: 'app-compatibilidad',
  imports: [RouterLink, CompatInfo, Cargando],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Compatibilidad</span>
          <h1>Lo que tu piel puede usar.</h1>
          <p>Cruzamos la ficha INCI de cada producto con tus alergias, tipo de piel y afecciones.</p>
        </div>
      </div>

      @if (error()) { <p class="error">{{ error() }}</p> }

      @if (cargando()) {
        <app-cargando [pasos]="pasosEvaluacion" />
      } @else if (perfilVacio()) {
        <p class="compat-aviso">Tu perfil dermatológico está vacío. Registra tus alergias, tipo de piel y afecciones
          para que podamos evaluar cada producto. <a routerLink="/perfil">Completar mi perfil →</a></p>
      } @else {
        <div class="compat-filtros" role="group" aria-label="Filtrar por compatibilidad">
          @for (f of filtros; track f.id) {
            <button type="button" class="btn-sec" [class.activo]="filtro() === f.id" [attr.aria-pressed]="filtro() === f.id"
                    (click)="filtro.set(f.id)">{{ f.label }} ({{ conteo()[f.id] }})</button>
          }
        </div>
        @if (visibles().length === 0) {
          <p class="nota">No hay productos en esta categoría.</p>
        } @else {
          <div class="product-grid">
            @for (p of visibles(); track p.id) {
              <article class="cat-card">
                <a class="cat-enlace" [routerLink]="['/producto', p.id]">
                  <div class="product-image">
                    @if (p.imagenes?.length) { <img [src]="p.imagenes[0]" [alt]="p.nombre" /> } @else { <span>✦</span> }
                  </div>
                  <h3 class="cat-nombre">{{ p.nombre }}</h3>
                </a>
                <p class="cat-marca">{{ p.marca || p.categoria || 'Cosmético' }}</p>
                <app-compat-info [resultado]="compat()[p.id]" />
              </article>
            }
          </div>
        }
      }
    </section>
  `,
  styles: [`
    .compat-filtros { display: flex; flex-wrap: wrap; gap: .5rem; margin: 1.5rem 0 1rem; }
    .compat-filtros .activo { background: var(--vino); color: #fff; border-color: var(--vino); }
  `],
})
export class Compatibilidad {
  private productos = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);

  readonly catalogo = signal<ProductoVendedor[]>([]);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly filtro = signal<Filtro>('todos');
  readonly pasosEvaluacion = [
    'Evaluando el catálogo contra tu perfil…',
    'Leyendo los ingredientes INCI…',
    'Cruzando con tus alergias…',
    'Revisando tu tipo de piel y afecciones…',
    'Midiendo pH y comedogenicidad…',
  ];
  readonly filtros: { id: Filtro; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'no-compatible', label: 'No compatibles' },
    { id: 'precaucion', label: 'Con precaución' },
    { id: 'compatible', label: 'Compatibles' },
    { id: 'recomendado', label: 'Recomendados' },
  ];

  /** Solo los productos evaluados, de los más riesgosos a los compatibles. */
  private readonly evaluados = computed(() =>
    this.catalogo()
      .filter((p) => this.compat()[p.id])
      .map((p) => ({ p, estado: estadoDe(this.compat()[p.id]) }))
      .sort((a, b) => ORDEN[a.estado] - ORDEN[b.estado]),
  );
  readonly visibles = computed(() =>
    this.evaluados().filter((e) => this.filtro() === 'todos' || e.estado === this.filtro()).map((e) => e.p),
  );
  readonly conteo = computed(() => {
    const c: Record<Filtro, number> = { todos: 0, 'no-compatible': 0, precaucion: 0, compatible: 0, recomendado: 0 };
    for (const e of this.evaluados()) { c.todos++; c[e.estado]++; }
    return c;
  });

  constructor() {
    this.productos.listarCatalogo().pipe(
      switchMap((ps) => {
        this.catalogo.set(ps);
        return this.compatibilidad.evaluar(ps.map((p) => p.id));
      }),
    ).subscribe({
      next: (e) => {
        this.perfilVacio.set(e.perfilVacio);
        this.compat.set(e.resultados);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
