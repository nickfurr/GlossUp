import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Página placeholder de la zona del cliente (catálogo, perfil, compatibilidad, carrito). */
@Component({
  selector: 'app-simple',
  template: `
    <section>
      <h2>{{ titulo }}</h2>
      <p class="nota">Sección en construcción.</p>
    </section>
  `,
})
export class Simple {
  readonly titulo = (inject(ActivatedRoute).snapshot.data['titulo'] as string) ?? '';
}
