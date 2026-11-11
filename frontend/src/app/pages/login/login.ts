import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, rutaDePanel } from '../../shared/auth';
import type { RolUsuario } from '../../shared/models';
import { EsperaReintento } from '../../shared/espera-reintento';

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
  private readonly dialogoSuspension = viewChild<ElementRef<HTMLDialogElement>>('dialogoSuspension');

  /** Motivo que escribió el administrador; se muestra en una ventana grande al intentar entrar. */
  readonly motivoSuspension = signal<string | null>(null);

  readonly esperaLogin = new EsperaReintento();
  readonly esperaRegistro = new EsperaReintento();
  readonly esperaRecuperar = new EsperaReintento();

  readonly esperaActual = computed(() => (this.esRegistro() ? this.esperaRegistro : this.esperaLogin));

  modo = signal<'login' | 'registro'>('login');
  readonly esRegistro = computed(() => this.modo() === 'registro');

  email = '';
  password = '';
  password2 = '';
  nombre = '';
  // El registro público solo crea clientes; los vendedores los crea un administrador.
  readonly rol: RolUsuario = 'CLIENTE';
  acepta = false;
  mayorDeEdad = false;

  error = signal<string | null>(null);
  info = signal<string | null>(null);
  cargando = signal(false);

  cambiarModo(): void {
    this.modo.set(this.esRegistro() ? 'login' : 'registro');
    this.error.set(null);
    this.info.set(null);
  }

  enviar(): void {
    if (this.esperaActual().bloqueado()) {
      return;
    }
    this.error.set(null);
    if (this.esRegistro() && !this.acepta) {
      this.error.set('Debes autorizar el tratamiento de tus datos para crear la cuenta.');
      return;
    }
    if (this.esRegistro() && !this.mayorDeEdad) {
      this.error.set('Debes ser mayor de 18 años para crear una cuenta.');
      return;
    }
    if (this.esRegistro()) {
      if (this.password.length < 8) {
        this.error.set('La contraseña debe tener al menos 8 caracteres.');
        return;
      }
      const tieneMayus = /[A-Z]/.test(this.password);
      const tieneMinus = /[a-z]/.test(this.password);
      const tieneNum = /[0-9]/.test(this.password);
      const tieneEspecial = /[^a-zA-Z0-9]/.test(this.password);
      if (!tieneMayus || !tieneMinus || !tieneNum || !tieneEspecial) {
        this.error.set('La contraseña debe combinar letras mayúsculas, minúsculas, números y caracteres especiales.');
        return;
      }
      if (this.password !== this.password2) {
        this.error.set('Las contraseñas no coinciden.');
        return;
      }
    }
    this.cargando.set(true);

    const entrar = () =>
      this.auth.login(this.email, this.password).subscribe({
        next: (s) => this.router.navigateByUrl(rutaDePanel(s.panel), { replaceUrl: true }),
        error: (e) => {
          this.cargando.set(false);
          if (this.esperaLogin.manejar(e)) {
            return;
          }
          // Vendedor registrado que el administrador aún no aprueba (auditoría OBS-T08): no es una suspensión.
          if (e?.status === 403 && !e.error?.detalles?.length && e.error?.message?.startsWith('Tu cuenta de vendedor')) {
            this.info.set(e.error.message);
            this.password = '';
            return;
          }
          if (e?.status === 403) {
            this.motivoSuspension.set(e.error?.detalles?.[0] ?? 'No se indicó un motivo.');
            this.password = '';
            this.dialogoSuspension()?.nativeElement.showModal();
            return;
          }
          this.error.set(this.msg(e, this.modo()));
        },
      });

    if (this.esRegistro()) {
      this.auth
        .registrar({ email: this.email, nombre: this.nombre, password: this.password, rol: this.rol, aceptaPolitica: this.acepta, mayorDeEdad: this.mayorDeEdad })
        .subscribe({
          next: () => entrar(),
          error: (e) => {
            this.cargando.set(false);
            if (this.esperaRegistro.manejar(e)) {
              return;
            }
            this.error.set(this.msg(e, 'registro'));
          },
        });
    } else {
      entrar();
    }
  }

  /** Usa el correo ya escrito en el formulario. El mensaje es igual exista o no la cuenta. */
  avisoOlvido(): void {
    if (this.esperaRecuperar.bloqueado()) {
      return;
    }
    this.error.set(null);
    this.info.set(null);
    const correo = this.email.trim();
    if (!correo) {
      this.error.set('Escribe tu correo arriba y vuelve a tocar "¿Olvidaste tu contraseña?".');
      return;
    }
    this.auth.recuperarClave(correo).subscribe({
      next: () => this.info.set('Si ese correo tiene cuenta, te enviamos un enlace para crear una contraseña nueva. Si no te llega en unos minutos, pídele el enlace al administrador.'),
      error: (e) => {
        if (this.esperaRecuperar.manejar(e)) {
          return;
        }
        this.error.set('Revisa que el correo esté bien escrito e inténtalo de nuevo.');
      },
    });
  }

  private msg(err: { status?: number; error?: { message?: string } }, modo: 'login' | 'registro'): string {
    const status = err?.status;
    const m = err?.error?.message;
    if (status === 429) return '';
    if (status === 401) return 'Correo o contraseña incorrectos.';
    if (status === 0 || status === 502 || status === 503 || status === 504) return 'No pudimos conectar con el servidor. Inténtalo de nuevo en un momento.';
    if (status === 422) return m ?? 'Ese correo ya está registrado.';
    if (status === 400) return 'Revisa los datos: la contraseña debe tener al menos 8 caracteres.';
    if (modo === 'registro' && (status === 404 || status === 405)) {
      return 'El registro aún no está habilitado en el servidor. Pídele a un administrador que cree tu cuenta.';
    }
    return modo === 'login' ? 'No se pudo iniciar sesión.' : 'No se pudo crear la cuenta.';
  }
}
