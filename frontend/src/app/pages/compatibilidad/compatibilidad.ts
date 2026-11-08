import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { CompatibilidadService, EstadoCompatibilidad, estadoDe } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { Cargando } from '../../shared/cargando';
import { ProductosService, mensajeDeError, paginarPorIds } from '../../shared/productos';
import type { ResultadoCompatibilidad } from '../../shared/models';

type Filtro = 'todos' | EstadoCompatibilidad;
/** Tope de ids por evaluación en el servidor. */
const LOTE_EVALUACION = 200;
const ORDEN: Record<EstadoCompatibilidad, number> = { 'no-compatible': 0, precaucion: 1, compatible: 2, recomendado: 3 };

/**
 * Compatibilidad producto–perfil (HU6): todo el catálogo publicado evaluado contra el perfil
 * dermatológico del cliente, agrupado en no compatibles, con precaución y compatibles. Se evalúan los ids del
 * índice; las fichas se bajan por tandas (auditoría R-14).
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
        @if (pagina.error()) {
          <p class="error" role="alert">{{ pagina.error() }}</p>
        } @else if (pagina.total() === 0) {
          <p class="nota">No hay productos en esta categoría.</p>
        } @else if (!visibles().length) {
          <app-cargando texto="Cargando productos…" />
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
          @if (pagina.hayMas()) {
            <div class="cargar-mas">
              <button type="button" class="btn-sec" [disabled]="pagina.cargando()" (click)="pagina.cargarMas()">
                {{ pagina.cargando() ? 'Cargando…' : 'Ver más productos (' + (pagina.total() - visibles().length) + ' más)' }}</button>
            </div>
          }
        }
      }
    </section>
  `,
  styles: [`
    .compat-filtros { display: flex; flex-wrap: wrap; gap: .5rem; margin: 1.5rem 0 1rem; }
    .cargar-mas { display: flex; justify-content: center; margin-top: 1.25rem; }
    .compat-filtros .activo { background: var(--vino); color: #fff; border-color: var(--vino); }
  `],
})
export class Compatibilidad {
  private productos = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);

  readonly ids = signal<number[]>([]);
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
    this.ids()
      .filter((id) => this.compat()[id])
      .map((id) => ({ id, estado: estadoDe(this.compat()[id]) }))
      .sort((a, b) => ORDEN[a.estado] - ORDEN[b.estado]),
  );
  private readonly idsFiltrados = computed(() =>
    this.evaluados().filter((e) => this.filtro() === 'todos' || e.estado === this.filtro()).map((e) => e.id),
  );
  readonly pagina = paginarPorIds(this.idsFiltrados);
  readonly visibles = this.pagina.visibles;
  readonly conteo = computed(() => {
    const c: Record<Filtro, number> = { todos: 0, 'no-compatible': 0, precaucion: 0, compatible: 0, recomendado: 0 };
    for (const e of this.evaluados()) { c.todos++; c[e.estado]++; }
    return c;
  });

  constructor() {
    this.productos.indice().pipe(
      switchMap((indice) => {
        const ids = indice.map((e) => e.id);
        const lotes = [];
        for (let i = 0; i < ids.length; i += LOTE_EVALUACION) lotes.push(ids.slice(i, i + LOTE_EVALUACION));
        return (lotes.length ? forkJoin(lotes.map((l) => this.compatibilidad.evaluar(l))) : of([])).pipe(
          map((evaluaciones) => ({ ids, evaluaciones })),
        );
      }),
    ).subscribe({
      next: ({ ids, evaluaciones }) => {
        this.perfilVacio.set(evaluaciones.some((e) => e.perfilVacio));
        this.compat.set(Object.assign({}, ...evaluaciones.map((e) => e.resultados)));
        this.ids.set(ids);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }
}
