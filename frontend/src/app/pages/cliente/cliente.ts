import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../shared/auth';

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
@Component({
  selector: 'app-cliente',
  imports: [RouterLink],
  template: `
    <section class="cliente-home">
      <div class="home-intro">
        <div>
          <span class="eyebrow">Belleza que entiende tu piel</span>
          <h1>Encuentra productos para tu Glow Up, {{ sesion()?.nombre }}.</h1>
          <p>Productos cosméticos seleccionados para acompañar tu bienestar.</p>
        </div>
      </div>
      <div class="home-search">
        <span aria-hidden="true">⌕</span>
        <input type="search" placeholder="Busca productos, marcas o ingredientes" aria-label="Buscar productos">
        <button type="button">Buscar</button>
      </div>
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
  readonly sesion = inject(AuthService).sesion;
  readonly productos = [1, 2, 3, 4, 5, 6];
}
