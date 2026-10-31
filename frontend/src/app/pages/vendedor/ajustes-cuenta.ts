import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../shared/auth';
import { mensajeDeError } from '../../shared/productos';

/** Ajustes de la cuenta del vendedor: nombre visible y contraseña. */
@Component({
  selector: 'app-ajustes-cuenta',
  imports: [FormsModule],
  template: `
    <div class="editor">
      <form class="panel" (ngSubmit)="guardarNombre()" novalidate>
        <h3>Tu cuenta</h3>
        <label class="campo">Correo
          <input [value]="sesion()?.email" disabled />
        </label>
        <label class="campo">Nombre
          <input name="nombre" [(ngModel)]="nombre" autocomplete="name" maxlength="150" />
        </label>
        @if (errorNombre()) { <p class="error" role="alert">{{ errorNombre() }}</p> }
        @if (avisoNombre()) { <p class="success-message" role="status">{{ avisoNombre() }}</p> }
        <div class="editor-acciones">
          <button type="submit" class="btn-primario" [disabled]="ocupado()">Guardar nombre</button>
        </div>
      </form>

      <form class="panel" (ngSubmit)="guardarClave()" novalidate>
        <h3>Cambiar contraseña</h3>
        <label class="campo">Contraseña actual
          <input name="actual" type="password" [(ngModel)]="actual" autocomplete="current-password" />
        </label>
        <label class="campo">Contraseña nueva
          <input name="nueva" type="password" [(ngModel)]="nueva" autocomplete="new-password" maxlength="120" />
          <span class="field-hint">Mínimo 8 caracteres.</span>
        </label>
        <label class="campo">Repite la contraseña nueva
          <input name="repetida" type="password" [(ngModel)]="repetida" autocomplete="new-password" maxlength="120" />
        </label>
        @if (errorClave()) { <p class="error" role="alert">{{ errorClave() }}</p> }
        @if (avisoClave()) { <p class="success-message" role="status">{{ avisoClave() }}</p> }
        <div class="editor-acciones">
          <button type="submit" class="btn-primario" [disabled]="ocupado()">Cambiar contraseña</button>
        </div>
      </form>
    </div>
  `,
})
export class AjustesCuenta {
  private auth = inject(AuthService);
  readonly sesion = this.auth.sesion;
  readonly ocupado = signal(false);
  readonly errorNombre = signal<string | null>(null);
  readonly avisoNombre = signal<string | null>(null);
  readonly errorClave = signal<string | null>(null);
  readonly avisoClave = signal<string | null>(null);
  nombre = this.auth.sesion()?.nombre ?? '';
  actual = '';
  nueva = '';
  repetida = '';

  guardarNombre(): void {
    this.errorNombre.set(null);
    this.avisoNombre.set(null);
    if (!this.nombre.trim()) {
      this.errorNombre.set('Escribe tu nombre.');
      return;
    }
    this.ocupado.set(true);
    this.auth.cambiarNombre(this.nombre.trim()).subscribe({
      next: () => {
        this.avisoNombre.set('Guardamos tu nombre.');
        this.ocupado.set(false);
      },
      error: (e) => {
        this.errorNombre.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  guardarClave(): void {
    this.errorClave.set(null);
    this.avisoClave.set(null);
    if (!this.actual) {
      this.errorClave.set('Escribe tu contraseña actual.');
    } else if (this.nueva.length < 8) {
      this.errorClave.set('La contraseña nueva debe tener al menos 8 caracteres.');
    } else if (this.nueva !== this.repetida) {
      this.errorClave.set('Las contraseñas nuevas no coinciden.');
    }
    if (this.errorClave()) return;
    this.ocupado.set(true);
    this.auth.cambiarClave(this.actual, this.nueva).subscribe({
      next: () => {
        this.actual = this.nueva = this.repetida = '';
        this.avisoClave.set('Cambiamos tu contraseña.');
        this.ocupado.set(false);
      },
      error: (e) => {
        this.errorClave.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }
}
