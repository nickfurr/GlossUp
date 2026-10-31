import { Component } from '@angular/core';
import { LibretaDirecciones } from '../../shared/libreta-direcciones';

/** "Mis direcciones" (HU14): administra la libreta fuera del checkout. */
@Component({
  selector: 'app-direcciones',
  imports: [LibretaDirecciones],
  template: `
    <section class="cliente-home pagina">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Tu cuenta</span>
          <h1>Mis direcciones</h1>
          <p>Cambiarlas no afecta los pedidos que ya hiciste: cada uno conserva la dirección con la que compraste.</p>
        </div>
      </div>
      <div class="panel"><app-libreta-direcciones /></div>
    </section>
  `,
  styles: [`.pagina { max-width: 46rem; }`],
})
export class Direcciones {}
