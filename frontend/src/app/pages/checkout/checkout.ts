import { Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CarritoService } from '../../shared/carrito';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { DireccionesService, fechaLlegada } from '../../shared/direcciones';
import { LibretaDirecciones } from '../../shared/libreta-direcciones';
import { Cargando } from '../../shared/cargando';
import { mensajeDeError } from '../../shared/productos';
import type { Direccion, LineaVista, OpcionEnvio } from '../../shared/models';

/**
 * "Finaliza tu compra" (HU14): una sola página, como el checkout de Mercado Libre. La dirección
 * principal ya viene elegida y se puede cambiar; el costo de envío se ve antes de pagar. Al pagar se
 * reserva el inventario (HU9) y se pasa a Wompi (HU10).
 */
@Component({
  selector: 'app-checkout',
  imports: [RouterLink, LibretaDirecciones, Cargando],
  template: `
    <section class="cliente-home checkout">
      <a routerLink="/carrito" class="back-link">← Volver al carrito</a>
      <h1>Finaliza tu compra</h1>

      @if (redirigiendo()) {
        <app-cargando texto="Reservamos tu inventario. Te llevamos a Wompi para pagar…" forma="filas" [cantidad]="2" />
      } @else if (cargando()) {
        <app-cargando texto="Preparando tu compra…" forma="filas" [cantidad]="3" />
      } @else if (lineas().length === 0) {
        <div class="panel">
          @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
          @else { <p>Tu carrito está vacío. <a routerLink="/catalogo" class="perfil-callout">Ir al catálogo</a></p> }
        </div>
      } @else {
        <div class="rejilla">
          <div class="bloques">
            <section class="panel" aria-labelledby="t-productos">
              <div class="encabezado">
                <h2 id="t-productos">{{ unidades() }} {{ unidades() === 1 ? 'producto' : 'productos' }} en tu compra</h2>
                <button type="button" class="enlace" [attr.aria-expanded]="verDetalle()" aria-controls="detalle-productos"
                        (click)="verDetalle.set(!verDetalle())">{{ verDetalle() ? 'Ocultar detalle' : 'Mostrar detalle' }}</button>
              </div>
              <div class="miniaturas" aria-hidden="true">
                @for (l of lineas(); track l.productoId) {
                  <span class="thumb">@if (l.imagen) { <img [src]="l.imagen" alt="" /> }</span>
                }
              </div>
              @if (verDetalle()) {
                <ul id="detalle-productos" class="detalle">
                  @for (l of lineas(); track l.productoId) {
                    <li><span>{{ l.nombre }} × {{ l.cantidad }}</span><strong>{{ cop.format(l.subtotal) }}</strong></li>
                  }
                </ul>
              }
            </section>

            <section class="panel" aria-labelledby="t-entrega">
              <h2 id="t-entrega">Forma de entrega</h2>
              @if (direccion() && !cambiando()) {
                <div class="elegida">
                  <span aria-hidden="true">📍</span>
                  <div>
                    <strong>{{ direccion()!.direccion }}{{ direccion()!.complemento ? ', ' + direccion()!.complemento : '' }}</strong>
                    <p>{{ direccion()!.barrio }}, {{ direccion()!.ciudad }}, {{ direccion()!.departamento }}</p>
                    <p class="suave">Recibe {{ direccion()!.nombreRecibe }} · {{ direccion()!.telefono }}</p>
                    <button type="button" class="enlace" (click)="cambiando.set(true)">Cambiar dirección</button>
                  </div>
                </div>
              }
              <div [hidden]="!!direccion() && !cambiando()">
                @if (direccion()) { <h3 class="elige">Elige una dirección</h3> }
                <app-libreta-direcciones [seleccionable]="true" [(seleccionada)]="direccion" (elegida)="cambiando.set(false)" />
              </div>

              @if (direccion() && !cambiando()) {
                @if (errorEnvio()) {
                  <p class="error" role="alert">{{ errorEnvio() }}</p>
                } @else {
                  <fieldset class="opciones">
                    <legend>¿Cómo quieres recibirlo?</legend>
                    @for (o of opciones(); track o.metodo) {
                      <label class="opcion" [class.activa]="metodo() === o.metodo">
                        <input type="radio" name="metodo" [checked]="metodo() === o.metodo" (change)="metodo.set(o.metodo)" />
                        <span class="opcion-texto"><strong>Llega el {{ llegada(o.fechaEstimada) }}</strong><span class="suave">Envío {{ o.etiqueta }}</span></span>
                        <strong [class.gratis]="o.costo === 0">{{ o.costo === 0 ? 'Gratis' : cop.format(o.costo) }}</strong>
                      </label>
                    }
                  </fieldset>
                }
              }
            </section>

            <section class="panel" aria-labelledby="t-pago">
              <h2 id="t-pago">Medio de pago</h2>
              <p class="pago">🔒 Eliges cómo pagar en la página segura de Wompi: tarjeta, PSE, Nequi o Bancolombia. GlossUP nunca ve los datos de tu tarjeta.</p>
            </section>
          </div>

          <aside class="panel resumen" aria-labelledby="t-resumen">
            <h2 id="t-resumen">Resumen de compra</h2>
            <dl>
              <div><dt>Productos ({{ unidades() }})</dt><dd>{{ cop.format(subtotal()) }}</dd></div>
              <div><dt>Envío</dt><dd [class.gratis]="costoEnvio() === 0">{{ opcion() ? (costoEnvio() === 0 ? 'Gratis' : cop.format(costoEnvio())) : '—' }}</dd></div>
              <div class="total"><dt>Total</dt><dd>{{ cop.format(total()) }}</dd></div>
            </dl>
            @if (noCompatibles() > 0) {
              <p class="compat-aviso" role="alert">⚠️ {{ noCompatibles() }} producto(s) no son compatibles con tu perfil. Si continúas, lo haces bajo tu propia responsabilidad.</p>
            }
            @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
            <button type="button" class="btn-primario ancho" [disabled]="ocupado() || !opcion() || cambiando()" (click)="pagar()">
              {{ ocupado() ? 'Reservando…' : 'Pagar y finalizar' }}
            </button>
            @if (!direccion()) { <p class="suave">Agrega una dirección para continuar.</p> }
            <p class="suave">Reservamos tu inventario 30 minutos mientras pagas.</p>
          </aside>
        </div>
      }
    </section>
  `,
  styles: [`
    .checkout h1 { margin: .5rem 0 1.2rem; color: var(--vino-osc); font-size: clamp(1.7rem, 3vw, 2.4rem); }
    .rejilla { display: grid; grid-template-columns: minmax(0, 1fr) 21rem; gap: 1.2rem; align-items: start; }
    .bloques { display: grid; gap: 1rem; min-width: 0; }
    h2 { margin: 0 0 .8rem; font-size: 1.1rem; color: var(--vino-osc); }
    .encabezado { display: flex; flex-wrap: wrap; justify-content: space-between; gap: .5rem; align-items: baseline; }
    .enlace { background: none; border: 0; padding: .2rem 0; color: var(--vino); font: inherit; font-weight: 700; cursor: pointer; text-decoration: underline; }
    .miniaturas { display: flex; flex-wrap: wrap; gap: .4rem; }
    .detalle { list-style: none; margin: .8rem 0 0; padding: 0; display: grid; gap: .45rem; font-size: .92rem; }
    .detalle li { display: flex; justify-content: space-between; gap: 1rem; }
    .elegida { display: flex; gap: .7rem; padding: .9rem 1rem; border: 1px solid var(--vino); border-radius: 12px; overflow-wrap: anywhere; }
    .elegida p { margin: .1rem 0; }
    .elige { margin: 0 0 .7rem; font-size: .95rem; }
    .suave { margin: .2rem 0 0; color: #7c6870; font-size: .86rem; }
    .opciones { border: 0; padding: 0; margin: 1.1rem 0 0; display: grid; gap: .5rem; }
    legend { padding: 0; margin-bottom: .5rem; font-weight: 700; }
    .opcion { display: flex; align-items: center; gap: .7rem; padding: .8rem 1rem; border: 1px solid var(--borde); border-radius: 12px; cursor: pointer; }
    .opcion.activa { border-color: var(--vino); box-shadow: 0 0 0 1px var(--vino); }
    .opcion input { width: 1.1rem; height: 1.1rem; flex: none; accent-color: var(--vino); }
    .opcion-texto { flex: 1; display: grid; }
    .gratis { color: var(--verde-ok); }
    .pago { margin: 0; }
    .resumen { position: sticky; top: 1rem; display: grid; gap: .7rem; }
    .resumen h2 { margin: 0; }
    dl { margin: 0; display: grid; gap: .5rem; }
    dl div { display: flex; justify-content: space-between; gap: 1rem; }
    dd { margin: 0; font-weight: 700; }
    .total { padding-top: .7rem; border-top: 1px solid var(--borde); font-size: 1.15rem; color: var(--vino-osc); }
    .total dt { font-weight: 800; }
    .resumen .compat-aviso { margin: 0; font-size: .85rem; }
    @media (max-width: 860px) {
      .rejilla { grid-template-columns: minmax(0, 1fr); }
      .resumen { position: static; }
    }
  `],
})
export class Checkout {
  private carrito = inject(CarritoService);
  private compatibilidad = inject(CompatibilidadService);
  private direcciones = inject(DireccionesService);
  private router = inject(Router);

  readonly cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  readonly llegada = fechaLlegada;

  readonly lineas = signal<LineaVista[]>([]);
  readonly cargando = signal(true);
  readonly ocupado = signal(false);
  readonly redirigiendo = signal(false);
  readonly error = signal<string | null>(null);
  readonly errorEnvio = signal<string | null>(null);
  readonly verDetalle = signal(false);
  readonly cambiando = signal(false);
  readonly direccion = signal<Direccion | null>(null);
  readonly opciones = signal<OpcionEnvio[]>([]);
  readonly metodo = signal<string | null>(null);
  readonly noCompatibles = signal(0);

  readonly unidades = computed(() => this.lineas().reduce((n, l) => n + l.cantidad, 0));
  readonly subtotal = computed(() => this.lineas().reduce((n, l) => n + l.subtotal, 0));
  readonly opcion = computed(() => (this.direccion() ? this.opciones().find((o) => o.metodo === this.metodo()) : undefined));
  readonly costoEnvio = computed(() => this.opcion()?.costo ?? 0);
  readonly total = computed(() => this.subtotal() + this.costoEnvio());

  constructor() {
    this.carrito.ver().subscribe({
      next: (c) => {
        this.carrito.detallar(c).subscribe((ls) => {
          this.lineas.set(ls);
          this.cargando.set(false);
        });
        if (c.items.length) {
          this.compatibilidad.evaluar(c.items.map((i) => i.productoId)).subscribe({
            next: (e) => this.noCompatibles.set(Object.values(e.resultados).filter((r) => r.compatible === false).length),
            error: () => {},
          });
        }
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });

    // Las formas de envío dependen del destino: se recotiza al cambiar de departamento.
    effect(() => {
      const departamento = this.direccion()?.departamento;
      const subtotal = this.subtotal();
      if (!departamento || !subtotal) return;
      this.errorEnvio.set(null);
      this.direcciones.opciones(departamento, subtotal).subscribe({
        next: (os) => {
          this.opciones.set(os);
          if (!os.some((o) => o.metodo === this.metodo())) this.metodo.set(os[0]?.metodo ?? null);
        },
        error: (e) => {
          this.opciones.set([]);
          this.errorEnvio.set(mensajeDeError(e));
        },
      });
    });
  }

  /** Reserva el inventario y lleva a Wompi. Si Wompi falla, el pedido ya existe y se paga desde Mis pedidos. */
  pagar(): void {
    const direccion = this.direccion();
    const opcion = this.opcion();
    if (!direccion || !opcion) return;
    this.ocupado.set(true);
    this.error.set(null);
    const items = this.lineas().map((l) => ({ productoId: l.productoId, cantidad: l.cantidad }));
    this.carrito.consolidar(items, direccion.id, opcion.metodo).subscribe({
      next: (pedido) => {
        this.carrito.vaciar().subscribe(); // el pedido ya reservó stock; dejamos el carrito limpio
        this.redirigiendo.set(true);
        this.carrito.iniciarPago(pedido.id).subscribe({
          next: ({ url }) => window.location.assign(url),
          error: () => this.router.navigate(['/pedidos'], { queryParams: { pedido: pedido.id } }),
        });
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }
}
