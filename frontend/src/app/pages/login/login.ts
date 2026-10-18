import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, rutaDePanel } from '../../shared/auth';
import type { RolUsuario } from '../../shared/models';

/**
 * Inicio de sesión y registro (mockups 3-4): tarjeta a dos columnas (panel de marca + formulario),
 * responsive. Tras autenticarse redirige al panel del rol. El backend no expone /register todavía:
 * el "Crear cuenta" muestra un aviso amable (404/405).
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  modo = signal<'login' | 'registro'>('login');
  readonly esRegistro = computed(() => this.modo() === 'registro');

  email = '';
  password = '';
  password2 = '';
  nombre = '';
  rol: RolUsuario = 'CLIENTE';
  acepta = false;

  error = signal<string | null>(null);
  info = signal<string | null>(null);
  cargando = signal(false);

  cambiarModo(): void {
    this.modo.set(this.esRegistro() ? 'login' : 'registro');
    this.error.set(null);
    this.info.set(null);
  }

  enviar(): void {
    this.error.set(null);
    if (this.esRegistro() && this.password !== this.password2) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    this.cargando.set(true);

    const entrar = () =>
      this.auth.login(this.email, this.password).subscribe({
        next: (s) => this.router.navigateByUrl(rutaDePanel(s.panel), { replaceUrl: true }),
        error: (e) => {
          this.error.set(this.msg(e, this.modo()));
          this.cargando.set(false);
        },
      });

    if (this.esRegistro()) {
      this.auth
        .registrar({ email: this.email, nombre: this.nombre, password: this.password, rol: this.rol })
        .subscribe({
          next: () => entrar(),
          error: (e) => {
            this.error.set(this.msg(e, 'registro'));
            this.cargando.set(false);
          },
        });
    } else {
      entrar();
    }
  }

  avisoOlvido(): void {
    this.info.set('Recuperación de contraseña: próximamente.');
  }
  avisoGoogle(): void {
    this.info.set('Inicio con Google: próximamente.');
  }

  private msg(err: { status?: number; error?: { message?: string } }, modo: 'login' | 'registro'): string {
    const status = err?.status;
    const m = err?.error?.message;
    if (status === 401) return 'Correo o contraseña incorrectos.';
    if (status === 422) return m ?? 'Ese correo ya está registrado.';
    if (status === 400) return 'Revisa los datos: la contraseña debe tener al menos 8 caracteres.';
    if (modo === 'registro' && (status === 404 || status === 405)) {
      return 'El registro aún no está habilitado en el servidor. Pídele a un administrador que cree tu cuenta.';
    }
    return modo === 'login' ? 'No se pudo iniciar sesión.' : 'No se pudo crear la cuenta.';
  }
}
