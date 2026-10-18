import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, rutaDePanel } from './auth';
import type { RolUsuario } from './models';

/**
 * Guard de ruta por rol: sin sesión redirige a /login; con un rol que no corresponde, al panel
 * propio. Cada panel "deja pasar" solo a su tipo de usuario.
 */
export function roleGuard(...roles: RolUsuario[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const s = auth.sesion();
    if (!s) return router.parseUrl('/login');
    if (!roles.includes(s.panel)) return router.parseUrl(rutaDePanel(s.panel));
    return true;
  };
}
