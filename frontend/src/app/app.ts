import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { SwUpdate } from '@angular/service-worker';
import { filter } from 'rxjs';
import { AuthService, rutaDePanel } from './shared/auth';

/**
 * Shell de la app: barra de navegación adaptada al rol + enrutado con guards. El vendedor usa su
 * propio layout con barra lateral, así que la barra superior solo se muestra a clientes y admins.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
})
export class App {
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly sesion = this.auth.sesion;
  readonly rutaPanel = computed(() => (this.sesion() ? rutaDePanel(this.sesion()!.panel) : '/login'));
  readonly mostrarTopbar = computed(
    () => !!this.sesion() && this.sesion()!.panel !== 'VENDEDOR' && this.sesion()!.panel !== 'ADMIN',
  );
  readonly claseMain = computed(() =>
    this.sesion()?.panel === 'VENDEDOR' || this.sesion()?.panel === 'ADMIN' ? 'full' : 'container',
  );
  readonly menuCuentaAbierto = signal(false);

  constructor() {
    // El service worker sirve la versión cacheada y baja la nueva en segundo plano: al tenerla
    // lista recargamos para que cada despliegue se vea sin tener que recargar dos veces.
    const sw = inject(SwUpdate);
    if (sw.isEnabled) {
      sw.versionUpdates
        .pipe(filter((e) => e.type === 'VERSION_READY'))
        .subscribe(() => document.location.reload());
    }
  }

  salir(): void {
    this.auth.salir();
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  alternarCuenta(): void {
    this.menuCuentaAbierto.update((abierto) => !abierto);
  }
}
