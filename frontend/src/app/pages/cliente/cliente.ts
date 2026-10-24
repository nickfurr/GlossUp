import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/auth';

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
@Component({
  selector: 'app-cliente',
  imports: [FormsModule],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Belleza que entiende tu piel</span>
          <h1>Encuentra productos para tu Glow Up, {{ sesion()?.nombre }}.</h1>
          <p>Productos cosméticos seleccionados para acompañar tu bienestar.</p>
        </div>
      </div>
      <form class="home-search" (ngSubmit)="buscar()">
        <span aria-hidden="true">⌕</span>
        <input type="search" name="q" [(ngModel)]="q" placeholder="Busca productos, marcas o ingredientes" aria-label="Buscar productos">
        <button type="submit">Buscar</button>
      </form>
      <div class="section-heading">
        <div><span class="eyebrow">Para ti</span><h2>Descubre productos</h2></div>
        <span class="catalog-note">Catálogo en actualización</span>
      </div>
      <div class="product-grid">
        @for (producto of productos; track producto) {
          <article class="product-placeholder">
            <div class="product-image"><span>✦</span></div>
            <div class="skeleton-line wide"></div><div class="skeleton-line"></div>
            <div class="skeleton-price"></div>
            <span class="placeholder-label">Próximamente</span>
          </article>
        }
      </div>
    </section>
  `,
})
export class Cliente {
  private router = inject(Router);
  readonly sesion = inject(AuthService).sesion;
  readonly productos = [1, 2, 3, 4, 5, 6];
  q = '';

  buscar(): void {
    this.router.navigate(['/catalogo'], { queryParams: this.q.trim() ? { q: this.q.trim() } : {} });
  }
}
