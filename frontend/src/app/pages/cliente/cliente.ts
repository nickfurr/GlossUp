import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../shared/auth';

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
@Component({
  selector: 'app-cliente',
  imports: [RouterLink],
  template: `
    <section>
      <h2>Hola, {{ sesion()?.nombre }} 👋</h2>
      <p>Este es tu espacio como <strong>cliente</strong> de GlossUP.</p>
      <div class="panel-grid">
        <a class="card tile" routerLink="/catalogo">🛍️ Explorar catálogo</a>
        <a class="card tile" routerLink="/compatibilidad">🧪 Analizar compatibilidad</a>
        <a class="card tile" routerLink="/perfil">🙂 Mi perfil dermatológico</a>
        <a class="card tile" routerLink="/carrito">🛒 Mi carrito</a>
      </div>
    </section>
  `,
})
export class Cliente {
  readonly sesion = inject(AuthService).sesion;
}
