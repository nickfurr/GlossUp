import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, switchMap } from 'rxjs';
import { PerfilService } from './perfil';
import type { PerfilDermatologico, ResultadoCompatibilidad } from './models';

/** Evaluación de compatibilidad para mi perfil: si está vacío no evalúa (no hay nada contra qué cruzar). */
export interface EvaluacionPerfil {
  perfilVacio: boolean;
  resultados: Record<number, ResultadoCompatibilidad>;
}

export type EstadoCompatibilidad = 'compatible' | 'precaucion' | 'no-compatible';

export function estadoDe(r: ResultadoCompatibilidad): EstadoCompatibilidad {
  if (!r.compatible) return 'no-compatible';
  return r.alertas.length ? 'precaucion' : 'compatible';
}

function perfilVacio(p: PerfilDermatologico): boolean {
  return !p.tipoPiel && p.alergias.length === 0 && p.afecciones.length === 0;
}

@Injectable({ providedIn: 'root' })
export class CompatibilidadService {
  private http = inject(HttpClient);
  private perfil = inject(PerfilService);

  /** Evalúa varios productos de una vez contra el perfil del cliente (HU6). */
  evaluar(productoIds: number[]): Observable<EvaluacionPerfil> {
    return this.perfil.obtener().pipe(
      switchMap((p) => {
        if (perfilVacio(p) || productoIds.length === 0) {
          return of({ perfilVacio: perfilVacio(p), resultados: {} });
        }
        return this.http
          .get<ResultadoCompatibilidad[]>('/api/compatibilidad/productos', { params: { ids: productoIds.join(',') } })
          .pipe(map((lista) => ({ perfilVacio: false, resultados: Object.fromEntries(lista.map((r) => [r.productoId, r])) })));
      }),
    );
  }
}
