import { Component, inject, input, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DireccionesService } from './direcciones';
import { Cargando } from './cargando';
import { mensajeDeError } from './productos';
import type { DatosDireccion, Direccion } from './models';

const VACIA: DatosDireccion = { nombreRecibe: '', telefono: '', departamento: '', ciudad: '', barrio: '', direccion: '',
  complemento: '', indicaciones: '', principal: false };

/**
 * Libreta de direcciones (HU14): agregar, editar, borrar y marcar la principal. En el checkout es
 * además el selector de "Elige una dirección"; sola, se administra desde "Mis direcciones".
 */
@Component({
  selector: 'app-libreta-direcciones',
  imports: [FormsModule, Cargando],
  template: `
    @if (cargando()) {
      <app-cargando texto="Buscando tus direcciones…" forma="filas" [cantidad]="2" />
    } @else if (formulario()) {
      <form class="form-direccion" (ngSubmit)="guardar()" novalidate>
        <h3 id="titulo-form">{{ editandoId() ? 'Editar dirección' : 'Nueva dirección' }}</h3>
        <div class="campos-2">
          <label class="campo">Nombre y apellido de quien recibe
            <input id="dir-nombre" name="nombreRecibe" [(ngModel)]="datos.nombreRecibe" autocomplete="name" maxlength="120"
                   [attr.aria-invalid]="!!errores()['nombreRecibe']" aria-describedby="err-nombreRecibe" />
            <span id="err-nombreRecibe" class="field-error">{{ errores()['nombreRecibe'] }}</span>
          </label>
          <label class="campo">Teléfono (+57)
            <input name="telefono" [(ngModel)]="datos.telefono" type="tel" inputmode="numeric" autocomplete="tel-national" maxlength="10"
                   [attr.aria-invalid]="!!errores()['telefono']" aria-describedby="err-telefono" />
            <span id="err-telefono" class="field-error">{{ errores()['telefono'] }}</span>
          </label>
          <label class="campo">Departamento
            <select name="departamento" [(ngModel)]="datos.departamento" autocomplete="address-level1"
                    [attr.aria-invalid]="!!errores()['departamento']" aria-describedby="err-departamento">
              <option value="">Selecciona un departamento</option>
              @for (d of departamentos(); track d) { <option [value]="d">{{ d }}</option> }
            </select>
            <span id="err-departamento" class="field-error">{{ errores()['departamento'] }}</span>
          </label>
          <label class="campo">Municipio o ciudad
            <input name="ciudad" [(ngModel)]="datos.ciudad" autocomplete="address-level2" maxlength="80"
                   [attr.aria-invalid]="!!errores()['ciudad']" aria-describedby="err-ciudad" />
            <span id="err-ciudad" class="field-error">{{ errores()['ciudad'] }}</span>
          </label>
          <label class="campo">Dirección
            <input name="direccion" [(ngModel)]="datos.direccion" autocomplete="address-line1" maxlength="160" placeholder="Ej.: Carrera 14 # 20-35"
                   [attr.aria-invalid]="!!errores()['direccion']" aria-describedby="err-direccion" />
            <span id="err-direccion" class="field-error">{{ errores()['direccion'] }}</span>
          </label>
          <label class="campo">Barrio
            <input name="barrio" [(ngModel)]="datos.barrio" autocomplete="address-level3" maxlength="80"
                   [attr.aria-invalid]="!!errores()['barrio']" aria-describedby="err-barrio" />
            <span id="err-barrio" class="field-error">{{ errores()['barrio'] }}</span>
          </label>
        </div>
        <label class="campo">Apartamento o casa (opcional)
          <input name="complemento" [(ngModel)]="datos.complemento" autocomplete="address-line2" maxlength="120" />
        </label>
        <label class="campo">Indicaciones para la entrega (opcional)
          <textarea name="indicaciones" [(ngModel)]="datos.indicaciones" maxlength="300" rows="2"
                    placeholder="Ej.: portón negro, dejar en portería"></textarea>
        </label>
        @if (puedeMarcarPrincipal()) {
          <label class="check"><input type="checkbox" name="principal" [(ngModel)]="datos.principal" /> Usar como dirección principal</label>
        }
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <div class="editor-acciones">
          @if (direcciones().length) { <button type="button" class="btn-sec" [disabled]="ocupado()" (click)="cerrarFormulario()">Cancelar</button> }
          <button type="submit" class="btn-primario" [disabled]="ocupado()">{{ ocupado() ? 'Guardando…' : 'Guardar dirección' }}</button>
        </div>
      </form>
    } @else {
      <ul class="lista-direcciones">
        @for (d of direcciones(); track d.id) {
          <li class="tarjeta" [class.elegida]="seleccionable() && seleccionada()?.id === d.id">
            @if (seleccionable()) {
              <label class="elegir">
                <input type="radio" name="direccion" [checked]="seleccionada()?.id === d.id" (change)="elegir(d)" />
                <span class="sr-only">Enviar a {{ d.direccion }}, {{ d.ciudad }}</span>
              </label>
            }
            <div class="datos">
              <strong>{{ d.direccion }}{{ d.complemento ? ', ' + d.complemento : '' }}</strong>
              @if (d.principal) { <span class="principal">Principal</span> }
              <span>{{ d.barrio }}, {{ d.ciudad }}, {{ d.departamento }}</span>
              <span class="suave">Recibe {{ d.nombreRecibe }} · {{ d.telefono }}</span>
              @if (d.indicaciones) { <span class="suave">{{ d.indicaciones }}</span> }
            </div>
            <div class="acciones">
              @if (porBorrar() === d.id) {
                <span>¿Borrarla?</span>
                <button type="button" class="enlace" [disabled]="ocupado()" (click)="porBorrar.set(null)">No</button>
                <button type="button" class="enlace peligro" [disabled]="ocupado()" (click)="eliminar(d)">Sí, borrar</button>
              } @else {
                <button type="button" class="enlace" (click)="editar(d)" [attr.aria-label]="'Editar ' + d.direccion">Editar</button>
                <button type="button" class="enlace" (click)="porBorrar.set(d.id)" [attr.aria-label]="'Borrar ' + d.direccion">Borrar</button>
              }
            </div>
          </li>
        }
      </ul>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      <button type="button" class="btn-sec" (click)="nueva()">+ Agregar nueva dirección</button>
    }
  `,
  styles: [`
    .lista-direcciones { list-style: none; margin: 0 0 .9rem; padding: 0; display: grid; gap: .6rem; }
    .tarjeta { display: flex; gap: .8rem; align-items: flex-start; padding: .9rem 1rem; border: 1px solid var(--borde); border-radius: 12px; background: #fff; }
    .tarjeta.elegida { border-color: var(--vino); box-shadow: 0 0 0 1px var(--vino); }
    .elegir { padding-top: .15rem; }
    .elegir input { width: 1.1rem; height: 1.1rem; accent-color: var(--vino); }
    .datos { flex: 1; min-width: 0; display: grid; gap: .15rem; overflow-wrap: anywhere; }
    .suave { color: #7c6870; font-size: .86rem; }
    .principal { justify-self: start; padding: .1rem .55rem; border-radius: 999px; background: var(--rosa-pal); color: var(--vino); font-size: .72rem; font-weight: 800; }
    .acciones { display: flex; flex-wrap: wrap; gap: .7rem; align-items: center; font-size: .85rem; }
    .enlace { background: none; border: 0; padding: .2rem; color: var(--vino); font: inherit; font-weight: 700; cursor: pointer; text-decoration: underline; }
    .peligro { color: #9b1c36; }
    h3 { margin: 0 0 .9rem; color: var(--vino-osc); }
    .campo select { width: 100%; padding: .6rem; border: 1px solid var(--borde); border-radius: 8px; font: inherit; font-weight: 400; background: #fff; }
    .campo [aria-invalid="true"] { border-color: #a53d59; }
    .field-error { margin: 0; }
    .field-error:empty { display: none; }
    .check { display: flex; gap: .5rem; align-items: center; font-size: .9rem; }
    .check input { accent-color: var(--vino); }
    @media (max-width: 640px) {
      .tarjeta { flex-wrap: wrap; }
      .datos { flex-basis: 70%; }
      .form-direccion .campos-2 { grid-template-columns: minmax(0, 1fr); gap: 0; }
    }
  `],
})
export class LibretaDirecciones {
  private servicio = inject(DireccionesService);

  /** Con selector: al entrar elige la principal y `elegida` avisa cuando el cliente toma una. */
  readonly seleccionable = input(false);
  readonly seleccionada = model<Direccion | null>(null);
  readonly elegida = output<void>();

  readonly direcciones = signal<Direccion[]>([]);
  readonly departamentos = signal<string[]>([]);
  readonly cargando = signal(true);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly errores = signal<Record<string, string>>({});
  readonly formulario = signal(false);
  readonly editandoId = signal<number | null>(null);
  readonly porBorrar = signal<number | null>(null);
  datos: DatosDireccion = { ...VACIA };

  constructor() {
    this.servicio.departamentos().subscribe((ds) => this.departamentos.set(ds));
    this.servicio.listar().subscribe({
      next: (ds) => {
        this.direcciones.set(ds);
        this.cargando.set(false);
        if (ds.length === 0) {
          this.nueva();
        } else if (this.seleccionable() && !this.seleccionada()) {
          this.seleccionada.set(ds[0]); // la lista trae la principal primero
        }
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  puedeMarcarPrincipal(): boolean {
    const actual = this.direcciones().find((d) => d.id === this.editandoId());
    return this.direcciones().length > 0 && !actual?.principal;
  }

  elegir(d: Direccion): void {
    this.seleccionada.set(d);
    this.elegida.emit();
  }

  nueva(): void {
    this.datos = { ...VACIA };
    this.abrirFormulario(null);
  }

  editar(d: Direccion): void {
    const { id: _id, ...datos } = d;
    this.datos = { ...datos, complemento: d.complemento ?? '', indicaciones: d.indicaciones ?? '' };
    this.abrirFormulario(d.id);
  }

  private abrirFormulario(id: number | null): void {
    this.editandoId.set(id);
    this.errores.set({});
    this.error.set(null);
    this.formulario.set(true);
    setTimeout(() => document.getElementById('dir-nombre')?.focus());
  }

  cerrarFormulario(): void {
    this.formulario.set(false);
  }

  private validar(): boolean {
    const d = this.datos;
    const e: Record<string, string> = {};
    if (!d.nombreRecibe.trim()) e['nombreRecibe'] = 'Escribe quién recibe.';
    if (!/^(3\d{9}|60\d{8})$/.test(d.telefono)) e['telefono'] = 'Escribe un celular o fijo de 10 dígitos, sin espacios.';
    if (!d.departamento) e['departamento'] = 'Selecciona un departamento.';
    if (!d.ciudad.trim()) e['ciudad'] = 'Escribe el municipio o ciudad.';
    if (!d.direccion.trim()) e['direccion'] = 'Escribe la dirección.';
    if (!d.barrio.trim()) e['barrio'] = 'Escribe el barrio.';
    this.errores.set(e);
    return Object.keys(e).length === 0;
  }

  guardar(): void {
    if (!this.validar()) return;
    this.ocupado.set(true);
    this.error.set(null);
    const id = this.editandoId();
    (id ? this.servicio.actualizar(id, this.datos) : this.servicio.crear(this.datos)).subscribe({
      next: (guardada) => this.recargar(() => {
        this.formulario.set(false);
        if (this.seleccionable()) this.elegir(guardada);
      }),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  eliminar(d: Direccion): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.servicio.eliminar(d.id).subscribe({
      next: () => this.recargar((ds) => {
        this.porBorrar.set(null);
        if (this.seleccionada()?.id === d.id) this.seleccionada.set(ds[0] ?? null);
        if (ds.length === 0) this.nueva();
      }),
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  /** La principal puede haber cambiado en otra dirección: siempre se relee la libreta. */
  private recargar(despues: (ds: Direccion[]) => void): void {
    this.servicio.listar().subscribe({
      next: (ds) => {
        this.direcciones.set(ds);
        this.ocupado.set(false);
        despues(ds);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }
}
