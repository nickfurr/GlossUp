import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/** Adjunta el token ****** y limpia la sesión si el backend responde 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
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
      if (error?.status === 401) {
        try {
          localStorage.removeItem('glossup_token');
        } catch {
          /* ignore */
        }
      }
      return throwError(() => error);
    }),
  );
};
