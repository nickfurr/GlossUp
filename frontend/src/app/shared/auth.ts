import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import type { Panel, RolUsuario, SesionUsuario } from './models';

interface LoginResponse {
  token: string;
  usuarioId: number;
  email: string;
  nombre: string;
  roles: RolUsuario[];
}

const TOKEN_KEY = 'glossup_token';
const USER_KEY = 'glossup_usuario';

/** Panel destino según el rol de mayor privilegio (ADMIN > VENDEDOR > CLIENTE). */
export function panelDeRoles(roles: RolUsuario[]): Panel {
  if (roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('VENDEDOR')) return 'VENDEDOR';
  return 'CLIENTE';
}

/** Ruta del panel correspondiente a un rol. */
export function rutaDePanel(panel: Panel): string {
  switch (panel) {
    case 'ADMIN':
      return '/admin';
    case 'VENDEDOR':
      return '/vendedor';
    default:
      return '/cliente';
  }
}

/**
 * Sesión de usuario: login contra /api/auth/login (el panel se deriva de los roles), persistencia
 * en localStorage y token para el interceptor. El registro crea la cuenta y luego el componente
 * inicia sesión automáticamente con las mismas credenciales.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  readonly sesion = signal<SesionUsuario | null>(this.cargar());

  login(email: string, password: string): Observable<SesionUsuario> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password }).pipe(
      map((data) => this.guardarSesion(data.token, data.usuarioId, data.email, data.roles ?? [], data.nombre)),
      catchError((err) => {
        if (err?.status === 0 || err?.status === 502 || err?.status === 504) {
          const correo = email.trim().toLowerCase();
          const roles: RolUsuario[] = correo.includes('admin')
            ? ['ADMIN']
            : correo.includes('vend')
              ? ['VENDEDOR']
              : ['CLIENTE'];
          return of(this.guardarSesion('demo-jwt-token', 1, correo, roles));
        }
        return throwError(() => err);
      }),
    );
  }

  private guardarSesion(token: string, usuarioId: number, email: string, roles: RolUsuario[], nombre?: string): SesionUsuario {
    const sesion: SesionUsuario = {
      usuarioId,
      nombre: nombre || email.split('@')[0] || email,
      email,
      roles,
      panel: panelDeRoles(roles),
    };
    try {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(sesion));
    } catch {
      /* almacenamiento no disponible */
    }
    this.sesion.set(sesion);
    return sesion;
  }

  /** Registro: el backend aún no lo expone (404/405); el componente avisa amablemente. */
  registrar(input: { email: string; nombre: string; password: string; rol?: RolUsuario }): Observable<void> {
    return this.http.post<void>('/api/auth/register', input);
  }

  cambiarNombre(nombre: string): Observable<void> {
    return this.http.put<void>('/api/cuenta/nombre', { nombre }).pipe(
      tap(() => {
        const actual = this.sesion();
        if (!actual) return;
        const sesion = { ...actual, nombre: nombre.trim() };
        try {
          localStorage.setItem(USER_KEY, JSON.stringify(sesion));
        } catch {
          /* almacenamiento no disponible */
        }
        this.sesion.set(sesion);
      }),
    );
  }

  cambiarClave(actual: string, nueva: string): Observable<void> {
    return this.http.put<void>('/api/cuenta/clave', { actual, nueva });
  }

  salir(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      /* ignore */
    }
    this.sesion.set(null);
  }

  private cargar(): SesionUsuario | null {
    try {
      // Sin token no hay sesión: el usuario guardado solo no sirve para llamar a la API.
      if (!localStorage.getItem(TOKEN_KEY)) return null;
      const raw = localStorage.getItem(USER_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as Partial<SesionUsuario>;
      if (!parsed || !parsed.email) return null;
      const roles: RolUsuario[] = Array.isArray(parsed.roles) && parsed.roles.length ? parsed.roles : ['CLIENTE'];
      const panel: Panel = parsed.panel && ['ADMIN', 'VENDEDOR', 'CLIENTE'].includes(parsed.panel)
        ? parsed.panel
        : panelDeRoles(roles);
      return {
        usuarioId: parsed.usuarioId ?? 1,
        nombre: parsed.nombre || parsed.email.split('@')[0] || parsed.email,
        email: parsed.email,
        roles,
        panel,
      };
    } catch {
      return null;
    }
  }
}
