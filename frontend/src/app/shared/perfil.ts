import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { OpcionesPerfil, PerfilDermatologico, TipoPiel } from './models';

const BASE = '/api/perfil/me';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private http = inject(HttpClient);

  obtener(): Observable<PerfilDermatologico> {
    return this.http.get<PerfilDermatologico>(BASE);
  }

  opciones(): Observable<OpcionesPerfil> {
    return this.http.get<OpcionesPerfil>(`${BASE}/opciones`);
  }

  actualizar(datos: { ph: number | null; tipoPiel: TipoPiel | null; tipoCabello: string | null }): Observable<PerfilDermatologico> {
    return this.http.put<PerfilDermatologico>(BASE, datos);
  }

  agregarAlergia(alergia: string): Observable<PerfilDermatologico> {
    return this.http.post<PerfilDermatologico>(`${BASE}/alergias`, { alergia });
  }

  quitarAlergia(alergia: string): Observable<PerfilDermatologico> {
    return this.http.delete<PerfilDermatologico>(`${BASE}/alergias`, { body: { alergia } });
  }

  agregarAfeccion(afeccion: string): Observable<PerfilDermatologico> {
    return this.http.post<PerfilDermatologico>(`${BASE}/afecciones`, { afeccion });
  }

  quitarAfeccion(afeccion: string): Observable<PerfilDermatologico> {
    return this.http.delete<PerfilDermatologico>(`${BASE}/afecciones`, { body: { afeccion } });
  }
  /** Autorización explícita para tratar los datos de salud (Ley 1581 art. 6). */
  autorizar(): Observable<PerfilDermatologico> {
    return this.http.post<PerfilDermatologico>(`${BASE}/autorizacion`, {});
  }
  /** Revoca la autorización y borra el perfil completo. */
  eliminar(): Observable<void> {
    return this.http.delete<void>(BASE);
  }
}
