import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { PerfilDermatologico, TipoCabello, TipoPiel } from './models';

const BASE = '/api/perfil/me';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private http = inject(HttpClient);

  obtener(): Observable<PerfilDermatologico> {
    return this.http.get<PerfilDermatologico>(BASE);
  }

  actualizar(datos: { ph: number | null; tipoPiel: TipoPiel | null; tipoCabello: TipoCabello | null }): Observable<PerfilDermatologico> {
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
}
