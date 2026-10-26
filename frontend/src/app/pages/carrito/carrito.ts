import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CarritoService } from '../../shared/carrito';
import { CompatibilidadService } from '../../shared/compatibilidad';
import { CompatInfo } from '../../shared/compat-info';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import type { Carrito as CarritoDto, LineaCompra, Pedido, ResultadoCompatibilidad } from '../../shared/models';

/** Línea del carrito enriquecida con nombre/imagen (el backend solo devuelve ids y montos). */
interface ItemVista extends LineaCompra {
  nombre: string;
  imagen?: string;
}

/**
 * Carrito del cliente (HU9 - SCRUM-36): CRUD de ítems + checkout que consolida y reserva
 * inventario. Los nombres se completan consultando cada producto (el carrito solo guarda ids).
 */
@Component({
  selector: 'app-carrito',
  imports: [RouterLink, CompatInfo],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Tu carrito</span>
          <h1>Revisa antes de reservar.</h1>
          <p>El inventario se reserva al confirmar el pedido.</p>
        </div>
      </div>

      @if (error()) {
        <p class="error" style="margin-bottom:1rem">{{ error() }}</p>
      }

      @if (pedido(); as ped) {
        <div class="panel">
          <h3>¡Pedido {{ ped.estado }}! 🎉</h3>
          <p>Pedido <strong>#{{ ped.id }}</strong> creado con inventario reservado.</p>
          <p class="precio" style="font-size:1.3rem">{{ formatearCop(ped.total) }}</p>
        </div>
      } @else if (cargando()) {
        <p class="loading">Cargando carrito…</p>
      } @else if (items().length === 0) {
        <p class="nota">Tu carrito está vacío. Ve al <a routerLink="/catalogo" class="perfil-callout">catálogo</a> para agregar productos.</p>
      } @else {
        <div class="panel">
          <table class="tabla">
            <thead>
              <tr><th>Producto</th><th>Precio</th><th>Cantidad</th><th>Subtotal</th><th></th></tr>
            </thead>
            <tbody>
              @for (it of items(); track it.productoId) {
                <tr>
                  <td>
                    <span class="celda-prod">
                      <span class="thumb">@if (it.imagen) {<img [src]="it.imagen" [alt]="it.nombre" />}</span>
                      <span>{{ it.nombre }}<app-compat-info [resultado]="compat()[it.productoId]" /></span>
                    </span>
                  </td>
                  <td class="precio">{{ formatearCop(it.precioUnitario) }}</td>
                  <td>
                    <div class="qty">
                      <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="cambiar(it, it.cantidad - 1)">−</button>
                      <span>{{ it.cantidad }}</span>
                      <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="cambiar(it, it.cantidad + 1)">+</button>
                    </div>
                  </td>
                  <td class="precio">{{ formatearCop(it.subtotal) }}</td>
                  <td><button type="button" class="quitar" [disabled]="ocupado()" (click)="quitar(it)" aria-label="Quitar">✕</button></td>
                </tr>
              }
            </tbody>
          </table>

          <div class="cart-pie">
            <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="vaciar()">Vaciar carrito</button>
            <div class="cart-total">
              <span>Total</span>
              <strong class="precio">{{ formatearCop(total()) }}</strong>
            </div>
          </div>
        </div>

        @if (noCompatibles() > 0) {
          <p class="compat-aviso" role="alert">⚠️ {{ noCompatibles() }} producto(s) de tu carrito no son compatibles con tu
            perfil dermatológico. Revisa las alertas antes de confirmar.</p>
        } @else if (perfilVacio()) {
          <p class="compat-aviso">Tu perfil está vacío: no podemos revisar alergias ni irritantes.
            <a routerLink="/perfil">Completa tu perfil →</a></p>
        }

        <div class="editor-acciones">
          <button type="button" class="btn-primario" [disabled]="ocupado()" (click)="confirmar()">
            Confirmar pedido y reservar inventario
          </button>
        </div>
      }
    </section>
  `,
  styles: [`
    .qty { display: flex; align-items: center; gap: .6rem; }
    .qty .btn-sec { padding: .2rem .7rem; line-height: 1; }
    .cart-pie { display: flex; justify-content: space-between; align-items: center; margin-top: 1rem; gap: 1rem; flex-wrap: wrap; }
    .cart-total { display: flex; align-items: baseline; gap: .6rem; }
    .cart-total strong { font-size: 1.4rem; }
  `],
})
export class Carrito {
  private carrito = inject(CarritoService);
  private productos = inject(ProductosService);
  private compatibilidad = inject(CompatibilidadService);
  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly items = signal<ItemVista[]>([]);
  readonly total = signal(0);
  readonly cargando = signal(true);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly pedido = signal<Pedido | null>(null);
  readonly compat = signal<Record<number, ResultadoCompatibilidad>>({});
  readonly perfilVacio = signal(false);
  readonly noCompatibles = computed(
    () => this.items().filter((i) => this.compat()[i.productoId]?.compatible === false).length,
  );

  constructor() {
    this.carrito.ver().subscribe({
      next: (c) => this.enriquecer(c),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  /** Completa cada línea con nombre e imagen del producto; si una consulta falla, usa "Producto #id". */
  private enriquecer(c: CarritoDto): void {
    this.total.set(c.total);
    if (c.items.length === 0) {
      this.items.set([]);
      this.cargando.set(false);
      return;
    }
    forkJoin(
      c.items.map((it) =>
        this.productos.obtener(it.productoId).pipe(
          map((p): ItemVista => ({ ...it, nombre: p.nombre, imagen: p.imagenes?.[0] })),
          catchError(() => of({ ...it, nombre: `Producto #${it.productoId}` } as ItemVista)),
        ),
      ),
    ).subscribe((lineas) => {
      this.items.set(lineas);
      this.cargando.set(false);
    });
    this.compatibilidad.evaluar(c.items.map((it) => it.productoId)).subscribe({
      next: (e) => {
        this.perfilVacio.set(e.perfilVacio);
        this.compat.set(e.resultados);
      },
      error: () => this.compat.set({}),
    });
  }

  private aplicar(c: CarritoDto): void {
    this.total.set(c.total);
    // Reusa los nombres ya cargados; las líneas nuevas caen a "Producto #id" hasta recargar.
    const previos = new Map(this.items().map((i) => [i.productoId, i]));
    this.items.set(
      c.items.map((it) => ({
        ...it,
        nombre: previos.get(it.productoId)?.nombre ?? `Producto #${it.productoId}`,
        imagen: previos.get(it.productoId)?.imagen,
      })),
    );
    this.ocupado.set(false);
  }

  private operar(obs: import('rxjs').Observable<CarritoDto>): void {
    this.ocupado.set(true);
    this.error.set(null);
    obs.subscribe({
      next: (c) => this.aplicar(c),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  cambiar(it: ItemVista, cantidad: number): void {
    if (cantidad < 1) {
      this.quitar(it);
      return;
    }
    this.operar(this.carrito.cambiarCantidad(it.productoId, cantidad));
  }

  quitar(it: ItemVista): void {
    this.operar(this.carrito.quitar(it.productoId));
  }

  vaciar(): void {
    this.operar(this.carrito.vaciar());
  }

  confirmar(): void {
    this.ocupado.set(true);
    this.error.set(null);
    const lineas = this.items().map((i) => ({ productoId: i.productoId, cantidad: i.cantidad }));
    this.carrito.consolidar(lineas).subscribe({
      next: (ped) => {
        this.pedido.set(ped);
        this.carrito.vaciar().subscribe(); // el pedido ya reservó stock; dejamos el carrito limpio
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }
}
