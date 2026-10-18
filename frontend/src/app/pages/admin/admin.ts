import { Component, inject } from '@angular/core';
import { AuthService } from '../../shared/auth';

/** Panel del ADMINISTRADOR: administración de la plataforma. */
@Component({
  selector: 'app-admin',
  template: `
    <section>
      <h2>Panel de administración — {{ sesion()?.nombre }}</h2>
      <p>Administración de la plataforma GlossUP.</p>
      <div class="panel-grid">
        <div class="card tile">👥 Usuarios y roles</div>
        <div class="card tile">🧬 Reglas de incompatibilidad <small>(ontología)</small></div>
        <div class="card tile">🏷️ Marcas y catálogos</div>
        <div class="card tile">📊 Métricas y reportes</div>
      </div>
      <p class="nota">Funcionalidad en construcción.</p>
    </section>
  `,
})
export class Admin {
  readonly sesion = inject(AuthService).sesion;
}
