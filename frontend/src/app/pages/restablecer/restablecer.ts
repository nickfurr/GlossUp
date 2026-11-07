import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/auth';
import { mensajeDeError } from '../../shared/productos';
import { EsperaReintento } from '../../shared/espera-reintento';

/** Crea una contraseña nueva con el enlace de un solo uso, pedido por correo o entregado por el administrador. */
@Component({
  selector: 'app-restablecer',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="panel restablecer">
      <h1>Crea tu contraseña nueva</h1>
      @if (listo()) {
        <p class="aviso-ok" role="status">Listo. Ya puedes entrar con tu contraseña nueva.</p>
        <a routerLink="/login" class="btn-primario">Ir a iniciar sesión</a>
      } @else if (!token) {
        <p class="error" role="alert">Este enlace está incompleto. Pide uno nuevo.</p>
        <a routerLink="/login" class="btn-sec">Volver</a>
      } @else {
        <form (ngSubmit)="guardar()" novalidate>
          <label class="campo">Contraseña nueva
            <input name="nueva" type="password" [(ngModel)]="nueva" autocomplete="new-password" maxlength="72" />
            <span class="field-hint">Entre 8 y 72 caracteres, con mayúsculas, minúsculas, números y caracteres especiales.</span>
          </label>
          <label class="campo">Repítela
            <input name="repetida" type="password" [(ngModel)]="repetida" autocomplete="new-password" maxlength="72" />
          </label>
          @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
          @if (espera.mensaje(); as m) { <p class="error" role="alert" aria-live="polite">{{ m }}</p> }
          <button type="submit" class="btn-primario" [disabled]="ocupado() || espera.bloqueado()">
            {{ ocupado() ? 'Guardando…' : espera.bloqueado() ? 'Espera…' : 'Guardar contraseña' }}
          </button>
        </form>
      }
    </section>
  `,
  styles: [`
    .restablecer { max-width: 26rem; margin: 3rem auto; display: grid; gap: .9rem; justify-items: start; }
    .restablecer h1 { margin: 0; font-size: 1.4rem; color: var(--vino-osc); }
    .restablecer form { width: 100%; }
  `],
})
export class Restablecer {
  private auth = inject(AuthService);
  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');
  readonly listo = signal(false);
  readonly ocupado = signal(false);
  readonly error = signal<string | null>(null);
  readonly espera = new EsperaReintento();
  nueva = '';
  repetida = '';

  guardar(): void {
    if (this.espera.bloqueado()) return;
    this.error.set(null);
    if (this.nueva.length < 8) {
      this.error.set('La contraseña debe tener al menos 8 caracteres.');
    } else if (!/[A-Z]/.test(this.nueva) || !/[a-z]/.test(this.nueva) || !/[0-9]/.test(this.nueva) || !/[^a-zA-Z0-9]/.test(this.nueva)) {
      this.error.set('La contraseña debe combinar mayúsculas, minúsculas, números y caracteres especiales.');
    } else if (this.nueva !== this.repetida) {
      this.error.set('Las contraseñas no coinciden.');
    }
    if (this.error() || !this.token) return;
    this.ocupado.set(true);
    this.auth.restablecerClave(this.token, this.nueva).subscribe({
      next: () => this.listo.set(true),
      error: (e) => {
        this.ocupado.set(false);
        if (this.espera.manejar(e)) {
          return;
        }
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
