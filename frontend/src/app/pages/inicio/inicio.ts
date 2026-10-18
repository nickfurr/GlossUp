import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, rutaDePanel } from '../../shared/auth';

/** Raíz: si hay sesión lleva a su panel; si no, al login. */
@Component({ selector: 'app-inicio', template: '' })
export class Inicio {
  constructor() {
    const s = inject(AuthService).sesion();
    inject(Router).navigateByUrl(s ? rutaDePanel(s.panel) : '/login', { replaceUrl: true });
  }
}
