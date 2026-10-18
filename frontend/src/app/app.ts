import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
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
  readonly mostrarTopbar = computed(() => !!this.sesion() && this.sesion()!.panel !== 'VENDEDOR');
  readonly claseMain = computed(() => (this.sesion()?.panel === 'VENDEDOR' ? 'full' : 'container'));

  salir(): void {
    this.auth.salir();
    this.router.navigate(['/login'], { replaceUrl: true });
  }
}
