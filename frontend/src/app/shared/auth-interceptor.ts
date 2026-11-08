import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth';

/**
 * La sesión viaja sola en la cookie HttpOnly. Si el backend responde 401 (sesión vencida o cerrada desde otro
 * dispositivo), olvida al usuario y manda a /login: dejarlo en pantalla hacía fallar en silencio todas las
 * llamadas (búsqueda, perfil, carrito).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error) => {
      // En /api/auth/** un 401 es "credenciales incorrectas": lo muestra el formulario de login.
      if (error?.status === 401 && !req.url.includes('/api/auth/')) {
        auth.olvidar();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
