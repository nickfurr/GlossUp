import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CarritoService } from '../../shared/carrito';
import { Cargando } from '../../shared/cargando';
import { PagoPendiente } from '../../shared/pago-pendiente';
import { mensajeDeError } from '../../shared/productos';
import type { EstadoTransaccion, Pedido } from '../../shared/models';

/** Cuántas veces se vuelve a consultar un pago que el banco aún no resuelve (cada 3 s ≈ 1 min). */
const MAX_CONSULTAS = 20;

/**
 * Página a la que Wompi devuelve al cliente (HU10): /pago/resultado/{pedido}?id={transacción}.
 * El estado se verifica en el servidor contra Wompi; el regreso por sí solo no prueba el pago.
 */
@Component({
  selector: 'app-pago-resultado',
  imports: [RouterLink, Cargando, PagoPendiente],
  template: `
    <section class="cliente-home resultado">
      @if (verificando()) {
        <app-cargando [pasos]="pasos" forma="filas" [cantidad]="1" />
      } @else if (error()) {
        <div class="panel estado estado-error" role="alert">
          <span class="icono" aria-hidden="true">!</span>
          <h1>No pudimos verificar tu pago</h1>
          <p>{{ error() }}</p>
          <p class="nota">Si Wompi te cobró, el pago se confirmará solo en unos minutos. No vuelvas a pagar todavía.</p>
          <div class="acciones">
            <button type="button" class="btn-primario" (click)="verificar()">Verificar de nuevo</button>
            <a routerLink="/carrito" class="btn-sec">Ir al carrito</a>
          </div>
        </div>
      } @else if (pedido(); as p) {
        @switch (vista()) {
          @case ('pagado') {
            <div class="panel estado estado-ok" role="status">
              <span class="icono" aria-hidden="true">✓</span>
              <h1>¡Pago aprobado!</h1>
              <p>Tu pedido <strong>#{{ p.id }}</strong> está pagado y lo estamos preparando.</p>
              <dl class="resumen">
                <dt>Total pagado</dt><dd>{{ cop.format(p.total) }}</dd>
                @if (p.metodoPago) { <dt>Medio de pago</dt><dd>{{ metodo(p.metodoPago) }}</dd> }
                @if (p.transaccionId) { <dt>Transacción Wompi</dt><dd>{{ p.transaccionId }}</dd> }
                @if (p.pagadoEn) { <dt>Fecha</dt><dd>{{ fecha(p.pagadoEn) }}</dd> }
              </dl>
              <div class="acciones"><a routerLink="/catalogo" class="btn-primario">Seguir comprando</a></div>
            </div>
          }
          @case ('en-proceso') {
            <div class="panel estado estado-espera" role="status">
              <span class="icono" aria-hidden="true">…</span>
              <h1>Tu banco aún no confirma el pago</h1>
              <p>Esto puede tardar unos minutos con PSE o Nequi. Te avisaremos aquí; no cierres esta página ni vuelvas a pagar.</p>
              <div class="acciones"><button type="button" class="btn-sec" (click)="verificar()">Verificar ahora</button></div>
            </div>
          }
          @case ('sin-pagar') {
            <div class="panel estado estado-error" role="alert">
              <span class="icono" aria-hidden="true">✕</span>
              @if (estado() === 'APROBADA') {
                <h1>No pudimos validar tu pago</h1>
                <p>Wompi aprobó una transacción que no coincide con este pedido. Escríbenos con el número de transacción y lo revisamos.</p>
              } @else {
                <h1>{{ estado() ? 'Tu pago no fue aprobado' : 'Tu pedido sigue sin pagar' }}</h1>
                <p>{{ estado() ? 'El banco o Wompi rechazó la transacción. No se te cobró nada.' : 'No recibimos un pago para este pedido.' }}
                  Puedes intentarlo otra vez con el mismo u otro medio de pago.</p>
              }
            </div>
            <app-pago-pendiente [pedido]="p" (cancelado)="pedido.set($event); estado.set(null)" />
          }
          @case ('cancelado') {
            <div class="panel estado estado-error" role="status">
              <span class="icono" aria-hidden="true">✕</span>
              <h1>Pedido #{{ p.id }} cancelado</h1>
              @if (estado() === 'APROBADA') {
                <p>Tu pago llegó después de que el pedido se canceló por tiempo. Te devolveremos el dinero; escríbenos con la transacción {{ transaccionId }}.</p>
              } @else {
                <p>El pedido se canceló y su inventario volvió al catálogo.</p>
              }
              <div class="acciones"><a routerLink="/catalogo" class="btn-primario">Ir al catálogo</a></div>
            </div>
          }
        }
      }
    </section>
  `,
  styles: [`
    .resultado { max-width: 40rem; }
    .estado { display: grid; gap: .6rem; justify-items: center; text-align: center; padding: 2rem 1.5rem; margin-bottom: 1rem; }
    .estado h1 { font-size: 1.6rem; margin: 0; }
    .estado p { margin: 0; }
    .icono { display: grid; place-items: center; width: 3.5rem; height: 3.5rem; border-radius: 50%; font-size: 1.8rem; font-weight: 800; color: #fff; animation: aparecer .4s ease; }
    .estado-ok .icono { background: #1f6b3a; }
    .estado-espera .icono { background: #c08a00; }
    .estado-error .icono { background: #9b1c36; }
    .resumen { display: grid; grid-template-columns: auto auto; gap: .35rem 1.5rem; margin: .5rem 0; text-align: left; }
    .resumen dt { color: #6b5a61; }
    .resumen dd { margin: 0; font-weight: 700; word-break: break-all; }
    .acciones { display: flex; flex-wrap: wrap; gap: .6rem; justify-content: center; margin-top: .5rem; }
    @keyframes aparecer { from { transform: scale(.6); opacity: 0; } to { transform: none; opacity: 1; } }
    @media (prefers-reduced-motion: reduce) { .icono { animation: none; } }
  `],
})
export class PagoResultado {
  private carrito = inject(CarritoService);
  private readonly pedidoId: number;
  readonly transaccionId: string | null;
  private consultas = 0;
  private espera?: ReturnType<typeof setTimeout>;

  readonly cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  readonly pedido = signal<Pedido | null>(null);
  readonly estado = signal<EstadoTransaccion | null>(null);
  readonly verificando = signal(true);
  /** Qué mostrar: manda el estado del pedido en GlossUP, no lo que diga la redirección. */
  readonly vista = computed(() => {
    const p = this.pedido();
    if (!p) return null;
    if (p.estado === 'PAGADO' || p.estado === 'ENVIADO') return 'pagado';
    if (p.estado === 'CANCELADO') return 'cancelado';
    return this.estado() === 'PENDIENTE' ? 'en-proceso' : 'sin-pagar';
  });
  readonly error = signal<string | null>(null);
  readonly pasos = ['Verificando tu pago con Wompi…', 'Consultando el estado de la transacción…', 'Casi listo…'];

  constructor() {
    const ruta = inject(ActivatedRoute).snapshot;
    this.pedidoId = Number(ruta.paramMap.get('pedido'));
    this.transaccionId = ruta.queryParamMap.get('id');
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
    this.verificar();
  }

  verificar(): void {
    clearTimeout(this.espera);
    this.error.set(null);
    this.verificando.set(this.pedido() === null);
    // Sin transacción el cliente volvió sin pagar: solo se muestra el pedido para pagarlo.
    if (!this.transaccionId) {
      this.carrito.obtenerPedido(this.pedidoId).subscribe({
        next: (p) => this.mostrar(p, null),
        error: (e) => this.fallar(e),
      });
      return;
    }
    this.carrito.confirmarPago(this.pedidoId, this.transaccionId).subscribe({
      next: (r) => {
        this.mostrar(r.pedido, r.estadoTransaccion);
        if (r.estadoTransaccion === 'PENDIENTE' && ++this.consultas < MAX_CONSULTAS) {
          this.espera = setTimeout(() => this.verificar(), 3000);
        }
      },
      error: (e) => this.fallar(e),
    });
  }

  private mostrar(p: Pedido, estado: EstadoTransaccion | null): void {
    this.pedido.set(p);
    this.estado.set(estado);
    this.verificando.set(false);
  }

  private fallar(e: unknown): void {
    this.error.set(mensajeDeError(e));
    this.verificando.set(false);
  }

  metodo(codigo: string): string {
    const nombres: Record<string, string> = { CARD: 'Tarjeta', PSE: 'PSE', NEQUI: 'Nequi', BANCOLOMBIA_TRANSFER: 'Botón Bancolombia',
      BANCOLOMBIA_QR: 'QR Bancolombia', DAVIPLATA: 'Daviplata' };
    return nombres[codigo] ?? codigo;
  }

  fecha(iso: string): string {
    return new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
