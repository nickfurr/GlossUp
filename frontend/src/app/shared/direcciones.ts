import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import type { DatosDireccion, Direccion, OpcionEnvio } from './models';

/** "2026-11-04" → "miércoles 4 de noviembre". El mediodía evita que el huso horario cambie el día. */
export function fechaLlegada(iso: string): string {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** Libreta de direcciones y cotización de envío (HU14). */
@Injectable({ providedIn: 'root' })
export class DireccionesService {
  private http = inject(HttpClient);

  listar(): Observable<Direccion[]> {
    return this.http.get<Direccion[]>('/api/direcciones');
  }

  crear(datos: DatosDireccion): Observable<Direccion> {
    return this.http.post<Direccion>('/api/direcciones', datos);
  }

  actualizar(id: number, datos: DatosDireccion): Observable<Direccion> {
    return this.http.put<Direccion>(`/api/direcciones/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/direcciones/${id}`);
  }

  departamentos(): Observable<string[]> {
    return this.http.get<string[]>('/api/envios/departamentos');
  }

  opciones(departamento: string, subtotal: number): Observable<OpcionEnvio[]> {
    return this.http.get<OpcionEnvio[]>('/api/envios/opciones', { params: { departamento, subtotal } });
  }
}
