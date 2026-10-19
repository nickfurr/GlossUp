import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PerfilService } from '../../shared/perfil';
import type { PerfilDermatologico, TipoCabello, TipoPiel } from '../../shared/models';
import { mensajeDeError } from '../../shared/productos';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  template: `
    <section class="profile-page">
      <a class="back-link" href="/cliente">← Volver a la tienda</a>
      <div class="profile-header">
        <div>
          <span class="eyebrow">Tu información de cuidado</span>
          <h1>Perfil dermatológico</h1>
          <p>Cuéntanos sobre tu piel para encontrar productos compatibles contigo.</p>
        </div>
        <div class="profile-status" [class.profile-status-complete]="completitud() === 100">
          <span aria-hidden="true">{{ completitud() === 100 ? '✓' : '◌' }}</span>
          {{ completitud() }}% completo
        </div>
      </div>
      <div class="profile-progress" aria-label="Completitud del perfil">
        <div class="profile-progress-bar" [style.width.%]="completitud()"></div>
      </div>
      @if (mensaje()) { <p class="success-message" role="status">{{ mensaje() }}</p> }
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      @if (cargando() && !perfil()) { <div class="card loading" aria-live="polite">Cargando tu perfil...</div> }
      @if (perfil(); as p) {
        <div class="profile-layout">
          <div class="card profile-form">
            <div class="section-title"><div><span class="section-kicker">01 · Biotipo</span><h2>Cuéntanos sobre ti</h2></div><span class="section-mark">✦</span></div>
            <p class="nota">Estos datos ayudan a filtrar productos adecuados para tu piel y cabello.</p>
            <label for="ph">pH de tu piel <span class="field-hint">Opcional · 0 a 14</span></label>
            <input id="ph" class="profile-input" type="number" min="0" max="14" step="0.1" [(ngModel)]="ph" placeholder="Ej. 5.2" aria-describedby="ph-hint">
            <span id="ph-hint" class="field-hint">Se guarda con una cifra decimal.</span>
            <label for="tipo-piel">Tipo de piel</label>
            <select id="tipo-piel" class="profile-input" [(ngModel)]="tipoPiel">
              <option [ngValue]="null">Selecciona una opción</option>
              @for (tipo of tiposPiel; track tipo) { <option [ngValue]="tipo">{{ etiqueta(tipo) }}</option> }
            </select>
            <label for="tipo-cabello">Tipo de cabello</label>
            <select id="tipo-cabello" class="profile-input" [(ngModel)]="tipoCabello">
              <option [ngValue]="null">Selecciona una opción</option>
              @for (tipo of tiposCabello; track tipo.codigo) {
                <option [ngValue]="tipo.codigo">{{ tipo.etiqueta }}</option>
              }
            </select>
            @if (errorBiotipo()) { <p class="field-error" role="alert">{{ errorBiotipo() }}</p> }
            <button class="btn-primario profile-save" type="button" [disabled]="cargando()" (click)="guardarBiotipo()">
              {{ cargando() ? 'Guardando...' : 'Guardar cambios' }}
            </button>
          </div>
          <aside class="card profile-summary">
            <span class="section-kicker">Tu resumen</span>
            <h2>Lo que sabemos de ti</h2>
            <div class="summary-stat"><span>pH</span><strong>{{ p.ph ?? 'Sin registrar' }}</strong></div>
            <div class="summary-stat"><span>Piel</span><strong>{{ p.tipoPiel ? etiqueta(p.tipoPiel) : 'Sin registrar' }}</strong></div>
            <div class="summary-stat"><span>Cabello</span><strong>{{ p.tipoCabello ? etiquetaCabello(p.tipoCabello) : 'Sin registrar' }}</strong></div>
            <div class="summary-stat"><span>Alergias</span><strong>{{ p.alergias.length }}</strong></div>
            <div class="summary-stat"><span>Afecciones</span><strong>{{ p.afecciones.length }}</strong></div>
            <p class="updated-at">{{ p.actualizadoEn ? 'Actualizado ' + fecha(p.actualizadoEn) : 'Aún no has guardado cambios' }}</p>
          </aside>
        </div>
        <div class="profile-lists">
          <div class="card tag-section">
            <div class="list-heading"><div><span class="section-kicker">02 · Ingredientes</span><h2>Alergias</h2><p class="nota">Añade ingredientes INCI que quieras evitar.</p></div><span class="count">{{ p.alergias.length }}</span></div>
            <div class="tag-list">@for (item of p.alergias; track item) { <span class="tag tag-danger">{{ item }} <button type="button" (click)="quitarAlergia(item)" aria-label="Quitar alergia {{ item }}">×</button></span> } @empty { <span class="empty">Todavía no registras alergias.</span> }</div>
            <div class="add-row"><label class="sr-only" for="nueva-alergia">Nueva alergia</label><input id="nueva-alergia" [(ngModel)]="nuevaAlergia" maxlength="120" placeholder="Ej. Parfum" (keyup.enter)="agregarAlergia()"><button class="btn-sec" type="button" [disabled]="cargando()" (click)="agregarAlergia()">Añadir</button></div>
            @if (errorAlergia()) { <p class="field-error" role="alert">{{ errorAlergia() }}</p> }
          </div>
          <div class="card tag-section">
            <div class="list-heading"><div><span class="section-kicker">03 · Condiciones</span><h2>Afecciones</h2><p class="nota">Registra condiciones para recomendaciones más precisas.</p></div><span class="count">{{ p.afecciones.length }}</span></div>
            <div class="tag-list">@for (item of p.afecciones; track item) { <span class="tag">{{ item }} <button type="button" (click)="quitarAfeccion(item)" aria-label="Quitar afección {{ item }}">×</button></span> } @empty { <span class="empty">Todavía no registras afecciones.</span> }</div>
            <div class="add-row"><label class="sr-only" for="nueva-afeccion">Nueva afección</label><input id="nueva-afeccion" [(ngModel)]="nuevaAfeccion" maxlength="120" placeholder="Ej. Rosácea" (keyup.enter)="agregarAfeccion()"><button class="btn-sec" type="button" [disabled]="cargando()" (click)="agregarAfeccion()">Añadir</button></div>
            @if (errorAfeccion()) { <p class="field-error" role="alert">{{ errorAfeccion() }}</p> }
          </div>
        </div>
      }
    </section>
  `,
})
export class Perfil {
  private service = inject(PerfilService);
  perfil = signal<PerfilDermatologico | null>(null);
  cargando = signal(false);
  error = signal<string | null>(null);
  mensaje = signal<string | null>(null);
  errorBiotipo = signal<string | null>(null);
  errorAlergia = signal<string | null>(null);
  errorAfeccion = signal<string | null>(null);
  ph: number | null = null;
  tipoPiel: TipoPiel | null = null;
  tipoCabello: TipoCabello | null = null;
  nuevaAlergia = '';
  nuevaAfeccion = '';
  readonly tiposPiel: TipoPiel[] = ['NORMAL', 'SECA', 'GRASA', 'MIXTA', 'SENSIBLE'];
  readonly tiposCabello: { codigo: TipoCabello; etiqueta: string }[] = [
    { codigo: 'LISO_1A', etiqueta: 'Liso 1A · fino y completamente liso' },
    { codigo: 'LISO_1B', etiqueta: 'Liso 1B · liso con más cuerpo' },
    { codigo: 'LISO_1C', etiqueta: 'Liso 1C · liso, grueso o resistente' },
    { codigo: 'ONDULADO_2A', etiqueta: 'Ondulado 2A · ondas suaves' },
    { codigo: 'ONDULADO_2B', etiqueta: 'Ondulado 2B · ondas marcadas' },
    { codigo: 'ONDULADO_2C', etiqueta: 'Ondulado 2C · ondas fuertes' },
    { codigo: 'RIZADO_3A', etiqueta: 'Rizado 3A · rizos amplios' },
    { codigo: 'RIZADO_3B', etiqueta: 'Rizado 3B · rizos definidos' },
    { codigo: 'RIZADO_3C', etiqueta: 'Rizado 3C · rizos cerrados' },
    { codigo: 'MUY_RIZADO_4A', etiqueta: 'Muy rizado 4A · espirales pequeñas' },
    { codigo: 'MUY_RIZADO_4B', etiqueta: 'Muy rizado 4B · patrón angular' },
    { codigo: 'MUY_RIZADO_4C', etiqueta: 'Muy rizado 4C · patrón muy cerrado' },
  ];

  constructor() { this.cargar(); }

  completitud(): number {
    const p = this.perfil();
    if (!p) return 0;
    let completado = 0;
    if (p.ph !== null) completado++;
    if (p.tipoPiel) completado++;
    if (p.tipoCabello) completado++;
    if (p.alergias.length || p.afecciones.length) completado++;
    return Math.round((completado / 4) * 100);
  }

  cargar(): void {
    this.cargando.set(true);
    this.service.obtener().subscribe({
      next: (p) => { this.perfil.set(p); this.ph = p.ph; this.tipoPiel = p.tipoPiel; this.tipoCabello = p.tipoCabello; this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }
  guardarBiotipo(): void {
    this.errorBiotipo.set(this.validarPh());
    if (this.errorBiotipo()) return;
    this.ejecutar(this.service.actualizar({ ph: this.ph, tipoPiel: this.tipoPiel, tipoCabello: this.tipoCabello }), 'Perfil actualizado correctamente.');
  }
  agregarAlergia(): void {
    this.errorAlergia.set(null);
    const value = this.nuevaAlergia.trim();
    if (!this.validarLista(value, 'alergia', this.perfil()?.alergias ?? [])) return;
    this.ejecutar(this.service.agregarAlergia(value), 'Alergia agregada.'); this.nuevaAlergia = '';
  }
  quitarAlergia(value: string): void { this.errorAlergia.set(null); this.ejecutar(this.service.quitarAlergia(value), 'Alergia eliminada.'); }
  agregarAfeccion(): void {
    this.errorAfeccion.set(null);
    const value = this.nuevaAfeccion.trim();
    if (!this.validarLista(value, 'afección', this.perfil()?.afecciones ?? [])) return;
    this.ejecutar(this.service.agregarAfeccion(value), 'Afección agregada.'); this.nuevaAfeccion = '';
  }
  quitarAfeccion(value: string): void { this.errorAfeccion.set(null); this.ejecutar(this.service.quitarAfeccion(value), 'Afección eliminada.'); }
  etiqueta(value: string): string { return value.charAt(0) + value.slice(1).toLowerCase(); }
  etiquetaCabello(value: TipoCabello): string { return this.tiposCabello.find((tipo) => tipo.codigo === value)?.etiqueta ?? value; }
  fecha(value: string): string { return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' }).format(new Date(value)); }
  private validarPh(): string | null {
    if (this.ph === null || this.ph === undefined) return null;
    if (!Number.isFinite(this.ph) || this.ph < 0 || this.ph > 14) return 'El pH debe estar entre 0 y 14.';
    return null;
  }
  private validarLista(value: string, nombre: string, existentes: string[]): boolean {
    const error = value.length === 0 ? `Escribe una ${nombre}.` : value.length > 120 ? `La ${nombre} no puede superar 120 caracteres.` : existentes.some((item) => item.toLowerCase() === value.toLowerCase()) ? `Esa ${nombre} ya está registrada.` : null;
    if (nombre === 'alergia') this.errorAlergia.set(error); else this.errorAfeccion.set(error);
    return !error;
  }
  private ejecutar(request: ReturnType<PerfilService['obtener']>, success: string): void {
    this.error.set(null); this.mensaje.set(null); this.cargando.set(true);
    request.subscribe({ next: (p) => { this.perfil.set(p); this.ph = p.ph; this.tipoPiel = p.tipoPiel; this.tipoCabello = p.tipoCabello; this.mensaje.set(success); this.cargando.set(false); }, error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); } });
  }
}
