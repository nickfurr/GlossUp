import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CarritoService } from '../../shared/carrito';
import { Cargando } from '../../shared/cargando';
import { PagoPendiente } from '../../shared/pago-pendiente';
import { DescargarComprobante } from '../../shared/descargar-comprobante';
import { mensajeDeError } from '../../shared/productos';
import type { Pedido } from '../../shared/models';

const ESTADOS: Record<string, { texto: string; clase: string }> = {
  CREADO: { texto: 'Pendiente de pago', clase: 'e-pendiente' },
  PAGADO: { texto: 'Pagado · en preparación', clase: 'e-pagado' },
  ENVIADO: { texto: 'Enviado', clase: 'e-enviado' },
  CANCELADO: { texto: 'Cancelado', clase: 'e-cancelado' },
};

const METODOS: Record<string, string> = { CARD: 'Tarjeta', PSE: 'PSE', NEQUI: 'Nequi', BANCOLOMBIA_TRANSFER: 'Botón Bancolombia',
  BANCOLOMBIA_QR: 'QR Bancolombia', DAVIPLATA: 'Daviplata' };

/**
 * Historial de compras del cliente (HU11): cada pedido con su estado, el detalle de productos, los
 * datos del pago y el comprobante en PDF. /pedidos?pedido={id} abre directamente ese pedido.
 */
@Component({
  selector: 'app-pedidos',
  imports: [RouterLink, Cargando, PagoPendiente, DescargarComprobante],
  template: `
    <section class="cliente-home pedidos">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Tu cuenta</span>
          <h1>Mis pedidos</h1>
          <p class="sub">Consulta el estado de tus compras y descarga sus comprobantes.</p>
        </div>
      </div>

      @if (cargando()) {
        <app-cargando [pasos]="['Buscando tus pedidos…', 'Ordenando tu historial…']" forma="filas" [cantidad]="3" />
      } @else if (error()) {
        <div class="panel"><p class="error" role="alert">{{ error() }}</p>
          <button type="button" class="btn-sec" (click)="cargar()">Reintentar</button></div>
      } @else if (pedidos().length === 0) {
        <div class="panel vacio">
          <span aria-hidden="true">🛍️</span>
          <h2>Aún no tienes pedidos</h2>
          <p>Cuando compres, aquí verás su estado y podrás descargar el comprobante.</p>
          <a routerLink="/catalogo" class="btn-primario">Ir al catálogo</a>
        </div>
      } @else {
        <ul class="lista">
          @for (p of pedidos(); track p.id) {
            <li class="panel pedido" [class.abierto]="abierto() === p.id">
              <button type="button" class="resumen" (click)="alternar(p.id)"
                      [attr.aria-expanded]="abierto() === p.id" [attr.aria-controls]="'detalle-' + p.id">
                <span class="titulo">
                  <strong>Pedido #{{ p.id }}</strong>
                  <span class="fecha">{{ fecha(p.creadoEn) }} · {{ unidades(p) }} {{ unidades(p) === 1 ? 'producto' : 'productos' }}</span>
                </span>
                <span class="estado" [class]="estado(p).clase">{{ estado(p).texto }}</span>
                <span class="total">{{ cop.format(p.total) }}</span>
                <span class="flecha" aria-hidden="true">⌄</span>
              </button>

              @if (abierto() === p.id) {
                <div class="detalle" [id]="'detalle-' + p.id">
                  <table>
                    <caption class="sr-only">Productos del pedido #{{ p.id }}</caption>
                    <thead><tr><th scope="col">Producto</th><th scope="col">Cant.</th><th scope="col">Precio</th><th scope="col">Subtotal</th></tr></thead>
                    <tbody>
                      @for (i of p.items; track i.productoId) {
                        <tr>
                          <td><a [routerLink]="['/producto', i.productoId]" class="cat-enlace">{{ i.nombre ?? 'Producto #' + i.productoId }}</a></td>
                          <td>{{ i.cantidad }}</td>
                          <td>{{ cop.format(i.precioUnitario) }}</td>
                          <td>{{ cop.format(i.subtotal) }}</td>
                        </tr>
                      }
                    </tbody>
                    <tfoot><tr><th scope="row" colspan="3">Total</th><td>{{ cop.format(p.total) }}</td></tr></tfoot>
                  </table>

                  @if (p.estado === 'PAGADO' || p.estado === 'ENVIADO') {
                    <div class="pago">
                      <dl>
                        @if (p.pagadoEn) { <div><dt>Pagado el</dt><dd>{{ fecha(p.pagadoEn) }}</dd></div> }
                        @if (p.metodoPago) { <div><dt>Medio de pago</dt><dd>{{ metodo(p.metodoPago) }}</dd></div> }
                        @if (p.transaccionId) { <div><dt>Transacción Wompi</dt><dd>{{ p.transaccionId }}</dd></div> }
                      </dl>
                      <app-descargar-comprobante [pedidoId]="p.id" />
                    </div>
                  } @else if (p.estado === 'CREADO') {
                    <app-pago-pendiente [pedido]="p" (cancelado)="reemplazar($event)" />
                  } @else {
                    <p class="nota-cancelado">Este pedido se canceló sin pago; su inventario volvió al catálogo.</p>
                  }
                </div>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: [`
    .pedidos { max-width: 52rem; }
    .sub { color: #8a7681; margin: .3rem 0 0; }
    .vacio { display: grid; gap: .6rem; justify-items: center; text-align: center; padding: 2.5rem 1rem; }
    .vacio > span { font-size: 2.2rem; }
    .vacio h2, .vacio p { margin: 0; }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .8rem; }
    .pedido { padding: 0; overflow: hidden; transition: box-shadow .2s ease; }
    .pedido.abierto { box-shadow: 0 10px 26px #5e1a3314; }
    .resumen { width: 100%; display: grid; grid-template-columns: 1fr auto auto auto; align-items: center; gap: 1rem;
      padding: 1rem 1.2rem; background: none; border: 0; font: inherit; color: inherit; text-align: left; cursor: pointer; }
    .resumen:hover { background: #fdf8fa; }
    .resumen:focus-visible { outline: 3px solid var(--vino); outline-offset: -3px; }
    .titulo { display: grid; gap: .15rem; }
    .fecha { color: #8a7681; font-size: .85rem; }
    .estado { padding: .3rem .7rem; border-radius: 999px; font-size: .78rem; font-weight: 800; white-space: nowrap; }
    .e-pendiente { background: #fdf4e1; color: #8a5f0c; }
    .e-pagado { background: #e9f5ee; color: var(--verde-ok); }
    .e-enviado { background: #e8f0fb; color: #2a5591; }
    .e-cancelado { background: #f3eef0; color: #7c6870; }
    .total { font-weight: 800; color: var(--vino-osc); white-space: nowrap; }
    .flecha { color: var(--vino); transition: transform .2s ease; }
    .abierto .flecha { transform: rotate(180deg); }
    .detalle { display: grid; gap: 1rem; padding: 0 1.2rem 1.2rem; border-top: 1px solid var(--borde); animation: abrir .2s ease; }
    table { width: 100%; border-collapse: collapse; margin-top: 1rem; font-size: .92rem; }
    th, td { padding: .55rem .4rem; text-align: right; border-bottom: 1px solid var(--borde); }
    th:first-child, td:first-child { text-align: left; }
    thead th { color: #8a7681; font-size: .78rem; text-transform: uppercase; letter-spacing: .6px; }
    tfoot th, tfoot td { border-bottom: 0; font-weight: 800; color: var(--vino-osc); font-size: 1rem; }
    .pago { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 1rem;
      padding: .9rem 1rem; border-radius: 12px; background: var(--crema); }
    dl { display: flex; flex-wrap: wrap; gap: .4rem 1.6rem; margin: 0; }
    dt { color: #8a7681; font-size: .75rem; }
    dd { margin: 0; font-weight: 700; word-break: break-all; }
    .nota-cancelado { margin: 0; color: #7c6870; }
    @keyframes abrir { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }
    @media (max-width: 640px) {
      .resumen { grid-template-columns: 1fr auto; }
      .estado { grid-row: 2; justify-self: start; }
      .flecha { display: none; }
    }
    @media (prefers-reduced-motion: reduce) { .detalle, .flecha, .pedido { animation: none; transition: none; } }
  `],
})
export class Pedidos {
  private carrito = inject(CarritoService);
  private readonly pedidoSolicitado = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('pedido')) || null;

  readonly cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  readonly pedidos = signal<Pedido[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly abierto = signal<number | null>(null);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.carrito.misPedidos().subscribe({
      next: (ps) => {
        this.pedidos.set(ps);
        // Abre el pedido pedido por la URL (desde el correo o el resultado del pago) o el más reciente.
        this.abierto.set(ps.find((p) => p.id === this.pedidoSolicitado)?.id ?? ps[0]?.id ?? null);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  alternar(id: number): void {
    this.abierto.update((actual) => (actual === id ? null : id));
  }

  reemplazar(p: Pedido): void {
    this.pedidos.update((ps) => ps.map((x) => (x.id === p.id ? p : x)));
  }

  estado(p: Pedido) {
    return ESTADOS[p.estado] ?? { texto: p.estado, clase: '' };
  }

  unidades(p: Pedido): number {
    return p.items.reduce((n, i) => n + i.cantidad, 0);
  }

  metodo(codigo: string): string {
    return METODOS[codigo] ?? codigo;
  }

  fecha(iso?: string | null): string {
    return iso ? new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '';
  }
}
