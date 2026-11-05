import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import type { Panel, RolUsuario, SesionUsuario } from './models';

/** El token no llega aquí: el servidor lo guarda en una cookie HttpOnly que este código no puede leer (auditoría OBS-T09). */
interface LoginResponse {
  usuarioId: number;
  email: string;
  nombre: string;
  roles: RolUsuario[];
}

/** Solo datos para pintar la sesión (nombre, rol). Ningún secreto vive en localStorage. */
const USER_KEY = 'glossup_usuario';
/** Donde las versiones anteriores guardaban el token; se borra si quedó. */
const TOKEN_VIEJO = 'glossup_token';

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
 * Sesión de usuario: login contra /api/auth/login (el panel se deriva de los roles). El servidor abre la sesión en
 * una cookie HttpOnly; aquí solo se recuerda quién es para la interfaz. El registro crea la cuenta y luego el
 * componente inicia sesión con las mismas credenciales (salvo vendedores, que esperan aprobación).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  readonly sesion = signal<SesionUsuario | null>(this.cargar());

  login(email: string, password: string): Observable<SesionUsuario> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password }).pipe(
      map((data) => this.guardarSesion(data.usuarioId, data.email, data.roles ?? [], data.nombre)),
    );
  }

  private guardarSesion(usuarioId: number, email: string, roles: RolUsuario[], nombre?: string): SesionUsuario {
    const sesion: SesionUsuario = {
      usuarioId,
      nombre: nombre || email.split('@')[0] || email,
      email,
      roles,
      panel: panelDeRoles(roles),
    };
    try {
      localStorage.removeItem(TOKEN_VIEJO);
      localStorage.setItem(USER_KEY, JSON.stringify(sesion));
    } catch {
      /* almacenamiento no disponible */
    }
    this.sesion.set(sesion);
    return sesion;
  }

  /** Registro: el backend aún no lo expone (404/405); el componente avisa amablemente. */
  registrar(input: { email: string; nombre: string; password: string; rol?: RolUsuario; aceptaPolitica: boolean; mayorDeEdad: boolean }): Observable<void> {
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

  /** Cierra las sesiones de los otros dispositivos; el servidor renueva la cookie de este. */
  cambiarClave(actual: string, nueva: string): Observable<void> {
    return this.http.put<void>('/api/cuenta/clave', { actual, nueva });
  }

  /** Pide el enlace de recuperación. La respuesta es igual exista o no el correo. */
  recuperarClave(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/recuperar', { email });
  }

  restablecerClave(token: string, nueva: string): Observable<void> {
    return this.http.post<void>('/api/auth/restablecer', { token, nueva });
  }

  /** Supresión de la cuenta por su titular (Ley 1581). Exige la contraseña; después cierra la sesión local. */
  eliminarCuenta(clave: string): Observable<void> {
    return this.http.post<void>('/api/cuenta/eliminar', { clave }).pipe(tap(() => this.salir()));
  }

  /** Cierra la sesión de este navegador: el servidor borra la cookie y aquí se olvida al usuario. */
  salir(): void {
    this.http.post<void>('/api/auth/logout', {}).subscribe({ error: () => { /* sin conexión: la cookie vence sola */ } });
    this.olvidar();
  }

  /** Cierra la sesión en todos los dispositivos de la cuenta, incluido este (auditoría OBS-T09). */
  cerrarTodasLasSesiones(): Observable<void> {
    return this.http.post<void>('/api/cuenta/cerrar-sesiones', {}).pipe(tap(() => this.olvidar()));
  }

  /** Solo el estado local: para cuando el servidor ya dijo que la sesión no vale (401). */
  olvidar(): void {
    try {
      localStorage.removeItem(TOKEN_VIEJO);
      localStorage.removeItem(USER_KEY);
    } catch {
      /* ignore */
    }
    this.sesion.set(null);
  }

  private cargar(): SesionUsuario | null {
    try {
      // Sesión de una versión anterior (token en localStorage): hay que volver a entrar para tener la cookie.
      if (localStorage.getItem(TOKEN_VIEJO)) {
        localStorage.removeItem(TOKEN_VIEJO);
        localStorage.removeItem(USER_KEY);
        return null;
      }
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
