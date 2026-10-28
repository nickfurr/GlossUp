import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of } from 'rxjs';
import { PerfilService } from './perfil';
import type { Cruce, PerfilDermatologico, ResultadoCompatibilidad, TipoPiel } from './models';

/** Evaluación de compatibilidad para mi perfil: si está vacío no evalúa (no hay nada contra qué cruzar). */
export interface EvaluacionPerfil {
  perfilVacio: boolean;
  perfil: PerfilDermatologico;
  resultados: Record<number, ResultadoCompatibilidad>;
}

export type EstadoCompatibilidad = 'recomendado' | 'compatible' | 'precaucion' | 'no-compatible';

export function estadoDe(r: ResultadoCompatibilidad): EstadoCompatibilidad {
  if (!r.compatible) return 'no-compatible';
  if (r.alertas.length) return 'precaucion';
  return r.recomendado ? 'recomendado' : 'compatible';
}

function perfilVacio(p: PerfilDermatologico): boolean {
  return !p.tipoPiel && p.alergias.length === 0 && p.afecciones.length === 0;
}

@Injectable({ providedIn: 'root' })
export class CompatibilidadService {
  private http = inject(HttpClient);
  private perfil = inject(PerfilService);

  /**
   * Evalúa varios productos de una vez contra el perfil del cliente (HU6). Perfil y evaluación se
   * piden en paralelo: esperar uno para pedir el otro sumaba un viaje completo al servidor.
   */
  evaluar(productoIds: number[]): Observable<EvaluacionPerfil> {
    if (productoIds.length === 0) {
      return this.perfil.obtener().pipe(map((p) => ({ perfilVacio: perfilVacio(p), perfil: p, resultados: {} })));
    }
    return forkJoin({
      p: this.perfil.obtener(),
      lista: this.http.get<ResultadoCompatibilidad[]>('/api/compatibilidad/productos', { params: { ids: productoIds.join(',') } }),
    }).pipe(
      map(({ p, lista }) => perfilVacio(p)
        ? { perfilVacio: true, perfil: p, resultados: {} }
        : { perfilVacio: false, perfil: p, resultados: Object.fromEntries(lista.map((r) => [r.productoId, r])) }),
    );
  }

  /** Cruces entre productos (HU8). No depende del perfil; con menos de dos productos no hay pares. */
  cruces(productoIds: number[]): Observable<Cruce[]> {
    if (new Set(productoIds).size < 2) return of([]);
    return this.http.get<Cruce[]>('/api/compatibilidad/cruces', { params: { ids: productoIds.join(',') } });
  }

  /** Filtros dermatológicos (HU13): ids del catálogo aptos para ese tipo de piel y/o sin mis alérgenos guardados. */
  filtrarCatalogo(tipoPiel: TipoPiel | null, excluirAlergenos: boolean): Observable<number[]> {
    const params: Record<string, string> = { excluirAlergenos: String(excluirAlergenos) };
    if (tipoPiel) params['tipoPiel'] = tipoPiel;
    return this.http.get<number[]>('/api/compatibilidad/catalogo', { params });
  }
}
