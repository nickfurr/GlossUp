import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth';

/**
 * Adjunta el token Bearer y, si el backend responde 401 (token vencido o inválido), cierra la
 * sesión completa y manda a /login: dejar el usuario en pantalla sin token hacía fallar en
 * silencio todas las llamadas (búsqueda, perfil, carrito).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  let token: string | null = null;
  try {
    token = localStorage.getItem('glossup_token');
  } catch {
    /* almacenamiento no disponible */
  }
  const autenticada = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(autenticada).pipe(
    catchError((error) => {
      // En /api/auth/** un 401 es "credenciales incorrectas": lo muestra el formulario de login.
      if (error?.status === 401 && !req.url.includes('/api/auth/')) {
        auth.salir();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
