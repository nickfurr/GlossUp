import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, forkJoin, of, switchMap, tap } from 'rxjs';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { CompatibilidadService, EvaluacionPerfil } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { Cargando } from '../../shared/cargando';
import { AgregarCarrito } from '../../shared/agregar-carrito';
import { DireccionesService, fechaLlegada } from '../../shared/direcciones';
import type { OpcionEnvio, ProductoVendedor } from '../../shared/models';

const MAX_CANTIDAD = 10;

/**
 * Ficha del producto, estilo marketplace: galería, caja de compra (precio, stock, cantidad,
 * comprar/agregar), descripción del vendedor, compatibilidad con el perfil (HU6–HU7),
 * características e ingredientes INCI marcados según el perfil.
 */
@Component({
  selector: 'app-producto',
  imports: [FormsModule, RouterLink, CompatInfo, Cargando, AgregarCarrito],
  template: `
    <section class="cliente-home ficha">
      <nav class="migas" aria-label="Ruta">
        <a routerLink="/catalogo">Catálogo</a>
        @if (producto()?.categoria; as c) { <span aria-hidden="true">›</span> <span>{{ c }}</span> }
        @if (producto(); as p) { <span aria-hidden="true">›</span> <span class="migas-actual">{{ p.nombre }}</span> }
      </nav>

      @if (cargando()) {
        <app-cargando texto="Cargando el producto…" [cantidad]="2" />
      } @else if (error()) {
        <p class="error">{{ error() }}</p>
        <a routerLink="/catalogo" class="perfil-callout">← Volver al catálogo</a>
      } @else if (producto(); as p) {
        <div class="ficha-principal panel">
          <div class="galeria">
            @if (p.imagenes.length > 1) {
              <div class="miniaturas" role="list" aria-label="Imágenes del producto">
                @for (img of p.imagenes; track $index) {
                  <button type="button" class="miniatura" [class.activa]="imagen() === $index" role="listitem"
                          (click)="imagen.set($index)" (mouseenter)="imagen.set($index)"
                          [attr.aria-label]="'Ver imagen ' + ($index + 1)">
                    <img [src]="img" alt="" />
                  </button>
                }
              </div>
            }
            <div class="imagen-grande" (mousemove)="enfocar($event)">
              @if (p.imagenes.length) {
                <img [src]="p.imagenes[imagen()]" [alt]="p.nombre" [style.transform-origin]="foco()" />
              } @else { <span>✦</span> }
            </div>
          </div>

          <div class="info">
            <p class="compra-marca">{{ p.marca || 'GlossUP' }}@if (p.categoria) { · {{ p.categoria }} }</p>
            <h1>{{ p.nombre }}</h1>
            <p class="compra-precio">{{ formatearCop(p.precio) }}</p>
            <app-compat-info [resultado]="compat()" />
            @if (p.ph) { <p class="dato">pH {{ p.ph }}</p> }
            @if (p.registroInvima) { <p class="dato">Registro INVIMA {{ p.registroInvima }}</p> }
          </div>

          <aside class="compra" aria-label="Comprar">
            @if (disponible()) {
              @if (entrega(); as e) {
                <p class="llega"><strong>Llega el {{ llegada(e.fechaEstimada) }}</strong>
                  <span>a {{ destino() }} · {{ e.costo === 0 ? 'Envío gratis' : 'Envío ' + formatearCop(e.costo) }}</span></p>
              } @else if (sinDireccion()) {
                <p class="llega"><a routerLink="/direcciones">Agrega tu dirección</a> para ver cuándo llega y cuánto cuesta el envío.</p>
              }
            }

            @if (!disponible()) {
              <p class="stock stock-agotado">{{ p.activo && !p.baneado ? 'Agotado' : 'Este producto ya no está a la venta' }}</p>
            } @else {
              <p class="stock" [class.stock-pocas]="p.stock <= 5">
                {{ p.stock <= 5 ? '¡Últimas ' + p.stock + ' unidades!' : 'Disponible · ' + p.stock + ' unidades' }}
              </p>
              <label class="cantidad">
                Cantidad
                <select [ngModel]="cantidad" (ngModelChange)="cantidad = $event; cotizarEntrega(p)" name="cantidad">
                  @for (n of opcionesCantidad(); track n) { <option [ngValue]="n">{{ n }} {{ n === 1 ? 'unidad' : 'unidades' }}</option> }
                </select>
              </label>
              <button type="button" class="btn-primario ancho" [disabled]="carro.agregando() === p.id" (click)="comprarAhora(p)">
                Comprar ahora
              </button>
              <button type="button" class="btn-sec ancho" [class.btn-agregado]="carro.agregado() === p.id"
                      [disabled]="carro.agregando() === p.id || carro.agregado() === p.id" (click)="agregar(p)">
                {{ carro.agregado() === p.id ? '✓ Agregado al carrito' : carro.agregando() === p.id ? 'Agregando…' : 'Agregar al carrito' }}
              </button>
            }

            <ul class="garantias">
              <li>🧪 Ficha de ingredientes INCI cruzada con tu perfil</li>
              <li>🔒 Reservamos tu inventario al confirmar el pedido</li>
            </ul>
          </aside>
        </div>

        <div class="ficha-detalle">
          <section class="panel">
            <h2>Descripción</h2>
            <p class="descripcion">{{ p.descripcion || 'El vendedor todavía no agregó una descripción para este producto.' }}</p>
          </section>

          <section class="panel">
            <h2>Compatibilidad con tu perfil</h2>
            @if (perfilVacio()) {
              <p class="compat-aviso">Completa tu perfil dermatológico para saber si este producto es para ti.
                <a routerLink="/perfil">Completar mi perfil →</a></p>
            } @else if (compat(); as r) {
              @if (r.alertas.length === 0 && r.beneficios.length === 0) {
                <p class="compat compat-compatible">✓ No encontramos nada en sus ingredientes que choque con tu perfil.</p>
              }
              @if (r.beneficios.length) {
                <h3 class="sub">Lo que le aporta a tu piel</h3>
                <ul class="lista-compat bueno">@for (b of r.beneficios; track $index) { <li>{{ b }}</li> }</ul>
              }
              @if (r.alertas.length) {
                <h3 class="sub">Lo que debes saber</h3>
                <ul class="lista-compat">
                  @for (a of r.alertas; track $index) {
                    <li [class]="'sev-' + a.severidad"><strong>{{ severidad[a.severidad] }}:</strong> {{ a.mensaje }}
                      <span class="compat-causa">Por qué: {{ a.causa }}</span></li>
                  }
                </ul>
              }
            } @else {
              <p class="nota">No pudimos evaluar este producto contra tu perfil.</p>
            }
          </section>

          <section class="panel">
            <h2>Características</h2>
            <table class="caracteristicas">
              <tbody>
                <tr><th>Marca</th><td>{{ p.marca || '—' }}</td></tr>
                <tr><th>Categoría</th><td>{{ p.categoria || '—' }}</td></tr>
                <tr><th>pH</th><td>{{ p.ph ?? 'No declarado' }}</td></tr>
                <tr><th>Registro INVIMA</th><td>{{ p.registroInvima || 'No declarado' }}</td></tr>
                <tr><th>Ingredientes</th><td>{{ p.ingredientes.length }}</td></tr>
              </tbody>
            </table>
          </section>

          <section class="panel">
            <h2>Ingredientes (INCI)</h2>
            <p class="nota">En el orden de la etiqueta.
              @if (!perfilVacio()) { <span class="leyenda"><span class="marca-alerta">⚠</span> choca con tu perfil · <span class="marca-bueno">✓</span> le hace bien a tu piel</span> }
            </p>
            <ol class="inci-lista">
              @for (i of p.ingredientes; track i.inci) {
                <li [class.inci-alerta]="marcas().alerta.has(i.inci)" [class.inci-bueno]="marcas().bueno.has(i.inci)">
                  <span class="inci-nombre">
                    @if (marcas().alerta.has(i.inci)) { <span class="marca-alerta" aria-label="Choca con tu perfil">⚠</span> }
                    @else if (marcas().bueno.has(i.inci)) { <span class="marca-bueno" aria-label="Le hace bien a tu piel">✓</span> }
                    {{ i.inci }}
                    @if (i.concentracion) { <span class="inci-conc">{{ i.concentracion }} %</span> }
                  </span>
                  @if (i.descripcion) { <span class="inci-desc">{{ i.descripcion }}</span> }
                </li>
              }
            </ol>
          </section>
        </div>
      }
    </section>

    <app-agregar-carrito #carro />
  `,
  styles: [`
    .migas { display: flex; flex-wrap: wrap; gap: .4rem; font-size: .85rem; color: #6b5a61; margin-bottom: 1rem; }
    .migas a { color: var(--vino); font-weight: 700; }
    .migas-actual { color: var(--tinta); }
    .ficha-principal { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) 19rem; gap: 1.5rem; align-items: start; padding: 1.25rem; }
    .galeria { display: flex; gap: .6rem; }
    .miniaturas { display: flex; flex-direction: column; gap: .5rem; flex: none; }
    .miniatura { width: 3.5rem; height: 3.5rem; padding: 2px; border: 1px solid var(--borde); border-radius: 6px; background: #fff; cursor: pointer; overflow: hidden; }
    .miniatura:hover { border-color: #c99aae; }
    .miniatura.activa { border: 2px solid var(--vino); padding: 1px; }
    .miniatura:focus-visible { outline: 3px solid var(--vino); outline-offset: 1px; }
    .miniatura img { display: block; width: 100%; height: 100%; object-fit: contain; }
    .imagen-grande { flex: 1; min-width: 0; aspect-ratio: 1; display: grid; place-items: center; background: #fff; overflow: hidden; font-size: 3rem; color: #c7839e; }
    .imagen-grande img { width: 100%; height: 100%; object-fit: contain; }
    @media (hover: hover) and (prefers-reduced-motion: no-preference) {
      .imagen-grande:hover img { transform: scale(1.8); cursor: zoom-in; }
    }
    .info { display: grid; gap: .6rem; min-width: 0; }
    .info h1 { font-size: 1.4rem; font-weight: 600; margin: 0; line-height: 1.25; overflow-wrap: anywhere; }
    .compra-marca { margin: 0; color: #6b5a61; font-size: .85rem; }
    .compra-precio { margin: .25rem 0; font-size: 2.1rem; font-weight: 500; color: var(--tinta); }
    .dato { margin: 0; color: #6b5a61; font-size: .85rem; }
    .compra { position: sticky; top: 1rem; display: grid; gap: .75rem; padding: 1rem; border: 1px solid var(--borde); border-radius: 10px; }
    .llega { display: grid; gap: .1rem; margin: 0; }
    .llega strong { color: var(--verde-ok); font-weight: 700; }
    .llega span { color: #6b5a61; font-size: .85rem; }
    .llega a { color: var(--vino); font-weight: 700; }
    .stock { margin: 0; font-weight: 700; color: #1f6b3a; }
    .stock-pocas { color: #8a5d00; }
    .stock-agotado { color: #9b1c36; }
    .cantidad { display: grid; gap: .3rem; font-size: .85rem; font-weight: 700; }
    .cantidad select { padding: .55rem .7rem; border: 1px solid var(--borde); border-radius: 10px; font: inherit; background: #fff; }
    .ancho { width: 100%; }
    .btn-agregado, .btn-agregado:disabled { background: #1f6b3a; border-color: #1f6b3a; color: #fff; opacity: 1; }
    .garantias { list-style: none; padding: .75rem 0 0; margin: 0; border-top: 1px solid var(--borde); display: grid; gap: .4rem; font-size: .85rem; color: #6b5a61; }
    .ficha-detalle { display: grid; gap: 1rem; margin-top: 1.5rem; }
    .ficha-detalle h2 { font-size: 1.15rem; margin: 0 0 .75rem; }
    .sub { font-size: .95rem; margin: .75rem 0 .4rem; }
    .descripcion { white-space: pre-line; line-height: 1.6; margin: 0; }
    .lista-compat { margin: 0; padding-left: 1.1rem; display: grid; gap: .5rem; }
    .lista-compat.bueno li::marker { content: '✓  '; color: #1f6b3a; }
    .caracteristicas { width: 100%; border-collapse: collapse; }
    .caracteristicas th, .caracteristicas td { text-align: left; padding: .6rem .75rem; border-bottom: 1px solid var(--borde); }
    .caracteristicas tr:nth-child(odd) { background: var(--crema); }
    .caracteristicas th { width: 40%; font-weight: 700; }
    .inci-lista { margin: 0; padding-left: 1.4rem; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: .5rem 1.5rem; }
    .inci-lista li { padding: .35rem .5rem; border-radius: 8px; }
    .inci-nombre { display: block; font-weight: 700; }
    .inci-desc { display: block; font-size: .8rem; color: #6b5a61; }
    .inci-conc { font-weight: 400; font-size: .8rem; color: #6b5a61; }
    .inci-alerta { background: #fdecef; }
    .inci-bueno { background: #eaf6ee; }
    .marca-alerta { color: #9b1c36; }
    .marca-bueno { color: #1f6b3a; }
    .leyenda { margin-left: .5rem; }
    @media (max-width: 1060px) {
      .ficha-principal { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }
      .galeria { grid-row: span 2; }
    }
    @media (max-width: 800px) {
      .ficha-principal { grid-template-columns: minmax(0, 1fr); padding: 1rem; }
      .galeria { grid-row: auto; flex-direction: column-reverse; }
      .compra { position: static; }
      .miniaturas { flex-direction: row; overflow-x: auto; padding-bottom: 2px; }
      .miniatura { flex: none; }
    }
  `],
})
export class Producto {
  private productos = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);
  private direcciones = inject(DireccionesService);
  private router = inject(Router);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  private readonly agregador = viewChild.required(AgregarCarrito);

  readonly producto = signal<ProductoVendedor | null>(null);
  readonly evaluacion = signal<EvaluacionPerfil | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly imagen = signal(0);
  readonly foco = signal('center');
  readonly entrega = signal<OpcionEnvio | null>(null);
  readonly destino = signal('');
  readonly sinDireccion = signal(false);
  readonly llegada = fechaLlegada;
  cantidad = 1;
  readonly severidad = { ALTA: 'Riesgo alto', MEDIA: 'Precaución', BAJA: 'Aviso' };

  readonly compat = computed(() => {
    const p = this.producto();
    return p ? this.evaluacion()?.resultados[p.id] : undefined;
  });
  readonly perfilVacio = computed(() => this.evaluacion()?.perfilVacio ?? false);
  readonly disponible = computed(() => {
    const p = this.producto();
    return !!p && p.activo && !p.baneado && p.stock > 0;
  });
  readonly opcionesCantidad = computed(() =>
    Array.from({ length: Math.min(this.producto()?.stock ?? 0, MAX_CANTIDAD) }, (_, i) => i + 1));

  /** Ingredientes nombrados en las alertas (⚠) o en los beneficios (✓) del motor. */
  readonly marcas = computed(() => {
    const p = this.producto();
    const r = this.compat();
    const alerta = new Set<string>();
    const bueno = new Set<string>();
    if (p && r) {
      for (const i of p.ingredientes) {
        const nombrado = new RegExp(`contiene ${i.inci.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}[ ,(]`, 'i');
        if (r.alertas.some((a) => nombrado.test(a.mensaje))) alerta.add(i.inci);
        else if (r.beneficios.some((b) => nombrado.test(b))) bueno.add(i.inci);
      }
    }
    return { alerta, bueno };
  });

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(
      tap(() => {
        this.cargando.set(true);
        this.error.set(null);
        this.imagen.set(0);
        this.cantidad = 1;
      }),
      switchMap((params) => {
        const id = Number(params.get('id'));
        // Un error en un producto no debe cortar la navegación a los siguientes.
        return forkJoin({ producto: this.productos.obtener(id), evaluacion: this.compatibilidad.evaluar([id]) }).pipe(
          catchError((e) => {
            this.error.set(mensajeDeError(e));
            this.cargando.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe(({ producto, evaluacion }) => {
      this.producto.set(producto);
      this.evaluacion.set(evaluacion);
      this.cargando.set(false);
      this.cotizarEntrega(producto);
    });
  }

  /** La lupa sigue al puntero sobre la imagen. */
  enfocar(e: MouseEvent): void {
    const zona = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.foco.set(`${((e.clientX - zona.left) / zona.width) * 100}% ${((e.clientY - zona.top) / zona.height) * 100}%`);
  }

  /** Promesa de entrega a la dirección principal. Si no se puede calcular, la ficha sigue sin ella. */
  cotizarEntrega(p: ProductoVendedor): void {
    this.direcciones.listar().pipe(
      switchMap(([principal]) => {
        this.sinDireccion.set(!principal);
        this.destino.set(principal?.ciudad ?? '');
        return principal ? this.direcciones.opciones(principal.departamento, p.precio * this.cantidad) : of([]);
      }),
      catchError(() => of([])),
    ).subscribe((opciones) => this.entrega.set(opciones[0] ?? null));
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  agregar(p: ProductoVendedor): void {
    this.agregador().intentar(p, this.compat(), this.cantidad);
  }

  comprarAhora(p: ProductoVendedor): void {
    this.agregador().intentar(p, this.compat(), this.cantidad, () => this.router.navigate(['/checkout']));
  }
}
