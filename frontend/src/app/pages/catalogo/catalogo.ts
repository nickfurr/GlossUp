import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { PerfilService } from '../../shared/perfil';
import { CompatInfo } from '../../shared/compat-info';
import { Cargando } from '../../shared/cargando';
import { AgregarCarrito } from '../../shared/agregar-carrito';
import type { PerfilDermatologico, ProductoVendedor, ResultadoCompatibilidad, TipoPiel } from '../../shared/models';

const TIPOS_PIEL: { valor: TipoPiel; texto: string }[] = [
  { valor: 'NORMAL', texto: 'Normal' }, { valor: 'SECA', texto: 'Seca' }, { valor: 'GRASA', texto: 'Grasa' },
  { valor: 'MIXTA', texto: 'Mixta' }, { valor: 'SENSIBLE', texto: 'Sensible' },
];

/**
 * Catálogo del cliente, estilo marketplace: carrusel "Recomendados para ti" (HU6: productos sin
 * alertas y con ingredientes buenos para su perfil), filtros por categoría y dermatológicos (HU13:
 * apto para un tipo de piel y sin mis alérgenos, resueltos en el servidor), búsqueda (HU12) y alta
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

      @if (!busqueda() && !cargando() && catalogo().length) {
        <div class="filtros-catalogo" role="search" aria-label="Filtros del catálogo">
          @if (categorias().length > 1) {
            <div class="grupo">
              <span class="grupo-titulo" id="f-cat">Categoría</span>
              <div class="pildoras" role="group" aria-labelledby="f-cat">
                @for (c of categoriasVisibles(); track c.nombre) {
                  <button type="button" class="pildora" [class.activa]="categoria() === c.nombre" [attr.aria-pressed]="categoria() === c.nombre"
                          (click)="categoria.set(c.nombre)">{{ c.nombre }} <span class="cuenta">{{ c.cantidad }}</span></button>
                }
                @if (categorias().length > MAX_CATEGORIAS) {
                  <button type="button" class="pildora mas" [attr.aria-expanded]="verTodas()" (click)="verTodas.set(!verTodas())">
                    {{ verTodas() ? 'Ver menos' : '+' + (categorias().length - MAX_CATEGORIAS) + ' más' }}</button>
                }
              </div>
            </div>
          }
          <div class="grupo">
            <span class="grupo-titulo" id="f-piel">Apto para piel</span>
            <div class="pildoras" role="group" aria-labelledby="f-piel">
              <button type="button" class="pildora" [class.activa]="!tipoPiel()" [attr.aria-pressed]="!tipoPiel()"
                      (click)="elegirPiel(null)">Cualquiera</button>
              @for (t of tiposPiel; track t.valor) {
                <button type="button" class="pildora" [class.activa]="tipoPiel() === t.valor" [attr.aria-pressed]="tipoPiel() === t.valor"
                        (click)="elegirPiel(t.valor)">{{ t.texto }}@if (perfil()?.tipoPiel === t.valor) { <span class="tuya">tu piel</span> }</button>
              }
            </div>
          </div>
          <div class="grupo grupo-alergenos">
            <label class="interruptor" [class.deshabilitado]="!alergenos().length">
              <input type="checkbox" role="switch" [checked]="sinAlergenos()" [disabled]="!alergenos().length"
                     (change)="alternarAlergenos($any($event.target).checked)" />
              <span class="riel" aria-hidden="true"></span>
              <span>Excluir mis alérgenos guardados
                @if (alergenos().length) { <small>({{ alergenos().join(', ') }})</small> }
                @else { <small>— no tienes alérgenos en tu perfil. <a routerLink="/perfil">Agregarlos</a></small> }
              </span>
            </label>
            @if (filtrosActivos()) {
              <button type="button" class="quitar" (click)="quitarFiltros()">✕ Quitar filtros</button>
            }
          </div>
          @if (tipoPiel()) {
            <p class="explica">Ocultamos los productos con ingredientes comedogénicos, irritantes o con un pH inadecuado para la piel {{ textoPiel() }}.</p>
          }
        </div>
      }

      @if (cargando() || filtrando()) {
        <app-cargando [texto]="busqueda() ? 'Buscando productos…' : filtrando() ? 'Aplicando tus filtros…' : 'Cargando productos…'" />
      } @else if (errorFiltro()) {
        <p class="error" role="alert">{{ errorFiltro() }} <button type="button" class="btn-sec" (click)="aplicarFiltros()">Reintentar</button></p>
      } @else if (visibles().length === 0) {
        @if (filtrosActivos() && !busqueda()) {
          <div class="sin-resultados"><p>Ningún producto de esta categoría cumple tus filtros.</p>
            <button type="button" class="btn-sec" (click)="quitarFiltros()">Quitar filtros</button></div>
        } @else {
          <p class="nota">No hay productos que coincidan.</p>
        }
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
    .filtros-catalogo { display: grid; gap: .9rem; margin-bottom: 1.3rem; padding: 1rem 1.1rem; background: #fff;
                        border: 1px solid var(--borde); border-radius: 16px; }
    .grupo { display: grid; grid-template-columns: 7.5rem 1fr; align-items: center; gap: .6rem; }
    .grupo-titulo { color: #8a7681; font-size: .74rem; font-weight: 800; text-transform: uppercase; letter-spacing: .8px; }
    .pildoras { display: flex; flex-wrap: wrap; gap: .4rem; }
    .pildora { display: inline-flex; align-items: center; gap: .35rem; padding: .42rem .85rem; border: 1px solid var(--borde);
               border-radius: 999px; background: var(--crema); color: var(--tinta); font: inherit; font-size: .86rem; font-weight: 600;
               cursor: pointer; transition: background .15s ease, border-color .15s ease, color .15s ease; }
    .pildora:hover { border-color: #c99aae; background: var(--rosa-pal); }
    .pildora:focus-visible { outline: 3px solid var(--vino); outline-offset: 2px; }
    .pildora.activa { background: var(--vino); border-color: var(--vino); color: #fff; }
    .cuenta { min-width: 1.4rem; padding: .05rem .4rem; border-radius: 999px; background: #fff; color: var(--vino); font-size: .72rem; font-weight: 800; text-align: center; }
    .pildora.activa .cuenta { background: #ffffff2e; color: #fff; }
    .tuya { padding: .05rem .4rem; border-radius: 999px; background: #e9f5ee; color: var(--verde-ok); font-size: .68rem; font-weight: 800; }
    .pildora.mas { background: none; border-style: dashed; color: var(--vino); font-weight: 800; }
    .pildora.activa .tuya { background: #ffffff2e; color: #fff; }
    .grupo-alergenos { grid-template-columns: 1fr auto; border-top: 1px dashed var(--borde); padding-top: .8rem; }
    .interruptor { display: inline-flex; align-items: center; gap: .65rem; font-weight: 700; font-size: .9rem; cursor: pointer; }
    .interruptor small { color: #8a7681; font-weight: 500; }
    .interruptor input { position: absolute; opacity: 0; width: 1px; height: 1px; }
    .riel { flex: none; position: relative; width: 2.6rem; height: 1.45rem; border-radius: 999px; background: #d9c7cf; transition: background .2s ease; }
    .riel::after { content: ''; position: absolute; top: .18rem; left: .18rem; width: 1.09rem; height: 1.09rem; border-radius: 50%;
                   background: #fff; box-shadow: 0 1px 3px #0003; transition: transform .2s ease; }
    .interruptor input:checked + .riel { background: var(--vino); }
    .interruptor input:checked + .riel::after { transform: translateX(1.15rem); }
    .interruptor input:focus-visible + .riel { outline: 3px solid var(--vino); outline-offset: 2px; }
    .interruptor.deshabilitado { cursor: default; opacity: .75; }
    .quitar { border: 0; background: none; color: var(--vino); font: inherit; font-weight: 800; cursor: pointer; white-space: nowrap; }
    .explica { margin: 0; color: #8a7681; font-size: .8rem; }
    .sin-resultados { display: grid; gap: .6rem; justify-items: start; }
    .sin-resultados p { margin: 0; color: #8a7681; }
    @media (max-width: 640px) {
      .grupo, .grupo-alergenos { grid-template-columns: 1fr; }
      .pildoras { flex-wrap: nowrap; overflow-x: auto; padding-bottom: .3rem; scrollbar-width: thin; }
      .pildora { flex: none; }
    }
    @media (prefers-reduced-motion: reduce) { .pildora, .riel, .riel::after { transition: none; } }
  `],
})
export class Catalogo {
  private api = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);
  private perfiles = inject(PerfilService);
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
  readonly tiposPiel = TIPOS_PIEL;
  readonly perfil = signal<PerfilDermatologico | null>(null);
  readonly tipoPiel = signal<TipoPiel | null>(null);
  readonly sinAlergenos = signal(false);
  /** Ids aptos según los filtros dermatológicos (HU13, calculados en el servidor); null = sin filtrar. */
  readonly aptos = signal<Set<number> | null>(null);
  readonly filtrando = signal(false);
  readonly errorFiltro = signal<string | null>(null);
  private consultaFiltro = 0;

  readonly filtrosActivos = computed(() => !!this.tipoPiel() || this.sinAlergenos());
  /** Código de alérgeno → nombre que el cliente eligió en su perfil (ej. "Bálsamo del Perú"). */
  private readonly etiquetas = signal<Record<string, string>>({});
  readonly alergenos = computed(() => (this.perfil()?.alergias ?? []).map((a) => this.etiquetas()[a] ?? legible(a)));
  readonly textoPiel = computed(() => TIPOS_PIEL.find((t) => t.valor === this.tipoPiel())?.texto.toLowerCase() ?? '');
  /** Productos que pasan los filtros dermatológicos; las categorías cuentan sobre ellos. */
  private readonly aptosCatalogo = computed(() => {
    const a = this.aptos();
    return a ? this.catalogo().filter((p) => a.has(p.id)) : this.catalogo();
  });
  readonly categorias = computed(() => {
    const ps = this.aptosCatalogo();
    const cuenta = new Map<string, number>();
    for (const p of this.catalogo()) if (p.categoria) cuenta.set(p.categoria, 0);
    for (const p of ps) if (p.categoria) cuenta.set(p.categoria, cuenta.get(p.categoria)! + 1);
    // Sin productos aptos la categoría no sirve de nada, salvo si es la elegida (para poder salir de ella).
    return [{ nombre: 'Todas', cantidad: ps.length }, ...[...cuenta]
      .filter(([nombre, cantidad]) => cantidad > 0 || nombre === this.categoria())
      .map(([nombre, cantidad]) => ({ nombre, cantidad }))];
  });
  readonly MAX_CATEGORIAS = 10;
  readonly verTodas = signal(false);
  /** Las primeras categorías, y la elegida aunque esté más abajo; el resto tras "+N más". */
  readonly categoriasVisibles = computed(() => {
    const cs = this.categorias();
    if (this.verTodas() || cs.length <= this.MAX_CATEGORIAS) return cs;
    const primeras = cs.slice(0, this.MAX_CATEGORIAS);
    const elegida = cs.find((c) => c.nombre === this.categoria());
    return elegida && !primeras.includes(elegida) ? [...primeras, elegida] : primeras;
  });
  readonly visibles = computed(() => {
    if (this.busqueda()) return this.resultados();
    const c = this.categoria();
    return c === 'Todas' ? this.aptosCatalogo() : this.aptosCatalogo().filter((p) => p.categoria === c);
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

  elegirPiel(t: TipoPiel | null): void {
    this.tipoPiel.set(t);
    this.aplicarFiltros();
  }

  alternarAlergenos(activo: boolean): void {
    this.sinAlergenos.set(activo);
    this.aplicarFiltros();
  }

  quitarFiltros(): void {
    this.tipoPiel.set(null);
    this.sinAlergenos.set(false);
    this.aplicarFiltros();
  }

  /** El servidor descarta los no aptos; si el cliente cambia de filtro antes de la respuesta, gana el último. */
  aplicarFiltros(): void {
    const consulta = ++this.consultaFiltro;
    this.errorFiltro.set(null);
    if (!this.filtrosActivos()) {
      this.aptos.set(null);
      this.filtrando.set(false);
      return;
    }
    this.filtrando.set(true);
    this.compatibilidad.filtrarCatalogo(this.tipoPiel(), this.sinAlergenos()).subscribe({
      next: (ids) => {
        if (consulta !== this.consultaFiltro) return;
        this.aptos.set(new Set(ids));
        this.filtrando.set(false);
      },
      error: (e) => {
        if (consulta !== this.consultaFiltro) return;
        this.errorFiltro.set(mensajeDeError(e));
        this.filtrando.set(false);
      },
    });
  }

  /** Una sola petición por lote; se acumula en el mapa. Si falla, el catálogo sigue usable sin sellos. */
  private evaluarCompatibilidad(ps: ProductoVendedor[]): void {
    if (!ps.length) { this.evaluando.set(false); return; }
    this.compatibilidad.evaluar(ps.map((p) => p.id)).subscribe({
      next: (e) => {
        this.perfilVacio.set(e.perfilVacio);
        this.perfil.set(e.perfil);
        if (e.perfil.alergias.length) {
          this.perfiles.opciones().subscribe((o) => this.etiquetas.set(Object.fromEntries(o.alergias.map((a) => [a.codigo, a.etiqueta]))));
        }
        this.compat.update((c) => ({ ...c, ...e.resultados }));
        this.evaluando.set(false);
      },
      error: () => this.evaluando.set(false),
    });
  }
}

/** PARABENOS → "Parabenos", ACEITES_ESENCIALES → "Aceites esenciales". */
function legible(codigo: string): string {
  const t = codigo.toLowerCase().replace(/_/g, ' ');
  return t.charAt(0).toUpperCase() + t.slice(1);
}
