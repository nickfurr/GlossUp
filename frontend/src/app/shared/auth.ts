import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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
      map((data) => {
        const roles = data.roles ?? [];
        const sesion: SesionUsuario = {
          usuarioId: data.usuarioId,
          nombre: data.nombre,
          email: data.email,
          roles,
          panel: panelDeRoles(roles),
        };
        try {
          localStorage.setItem(TOKEN_KEY, data.token);
          localStorage.setItem(USER_KEY, JSON.stringify(sesion));
        } catch {
          /* almacenamiento no disponible */
        }
        this.sesion.set(sesion);
        return sesion;
      }),
    );
  }

  /** Registra una cuenta pública; el componente hace login después de recibir 201. */
  registrar(input: { email: string; nombre: string; password: string; rol?: RolUsuario }): Observable<void> {
    return this.http.post<void>('/api/auth/register', input);
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
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as SesionUsuario) : null;
    } catch {
      return null;
    }
  }
}
