import { Injectable, Signal, computed, effect, inject, linkedSignal, signal, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, switchMap } from 'rxjs';
import type { DatosProducto, IngredienteCatalogo, LecturaEtiqueta, ProductoVendedor, VentasVendedor } from './models';

const BASE = '/api/catalogo/productos';
/** Productos por tanda en las listas largas (auditoría R-14). */
export const POR_PAGINA = 24;

/** Id y categoría de un producto publicado: el índice liviano del catálogo. */
export interface EntradaIndice {
  id: number;
  categoria?: string | null;
}

/** Producto recomendado, con la razón principal que explica por qué (auditoría NC-T10). */
export type ProductoRecomendado = ProductoVendedor & { razon: string };

interface Pagina {
  items: ProductoVendedor[];
  total: number;
}

/** Catálogo del vendedor. Crear/editar envían el producto completo; eliminar es borrado lógico. */
@Injectable({ providedIn: 'root' })
export class ProductosService {
  private http = inject(HttpClient);

  listarMisProductos(): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(`${BASE}/mis-productos`);
  }

  /** Id y categoría de todo el catálogo publicado, para contar y filtrar sin bajar las fichas. */
  indice(): Observable<EntradaIndice[]> {
    return this.http.get<EntradaIndice[]>(`${BASE}/indice`);
  }

  /** Fichas de estos productos (los que sigan publicados), en el mismo orden. Hasta 100 por llamada. */
  porIds(ids: number[]): Observable<ProductoVendedor[]> {
    if (!ids.length) return of([]);
    return this.http.get<Pagina>(BASE, { params: { ids: ids.join(',') } }).pipe(map((p) => p.items));
  }

  /** Recomendados para el cliente según su perfil y sus compras, con la razón de cada uno. */
  recomendados(limite: number): Observable<{ perfilVacio: boolean; productos: ProductoRecomendado[] }> {
    return this.http
      .get<{ perfilVacio: boolean; recomendaciones: { productoId: number; razon: string }[] }>('/api/recomendaciones', {
        params: { limite },
      })
      .pipe(
        switchMap((r) => {
          const razon = new Map(r.recomendaciones.map((x) => [x.productoId, x.razon]));
          return this.porIds([...razon.keys()]).pipe(
            map((ps) => ({ perfilVacio: r.perfilVacio, productos: ps.map((p) => ({ ...p, razon: razon.get(p.id)! })) })),
          );
        }),
      );
  }

  obtener(id: number): Observable<ProductoVendedor> {
    return this.http.get<ProductoVendedor>(`${BASE}/${id}`);
  }

  /** Búsqueda básica por palabra clave en nombre/marca/categoría (HU12). */
  buscar(q: string): Observable<ProductoVendedor[]> {
    return this.http.get<ProductoVendedor[]>(`${BASE}/buscar`, { params: { q } });
  }

  /** Busca en el catálogo cerrado de ingredientes por INCI o nombre en español. */
  buscarIngredientes(q: string): Observable<IngredienteCatalogo[]> {
    return this.http.get<IngredienteCatalogo[]>('/api/catalogo/ingredientes', { params: { q } });
  }

  crear(d: DatosProducto): Observable<ProductoVendedor> {
    return this.http.post<ProductoVendedor>(BASE, d);
  }

  actualizar(id: number, d: DatosProducto): Observable<ProductoVendedor> {
    return this.http.put<ProductoVendedor>(`${BASE}/${id}`, d);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }

  /** Vuelve a publicar un producto dado de baja. */
  publicar(id: number): Observable<ProductoVendedor> {
    return this.http.post<ProductoVendedor>(`${BASE}/${id}/publicar`, {});
  }

  /** Lee los ingredientes de la foto de una etiqueta (HU5). */
  leerEtiqueta(imagen: File): Observable<LecturaEtiqueta> {
    const datos = new FormData();
    datos.append('imagen', imagen);
    return this.http.post<LecturaEtiqueta>(`${BASE}/extraer-ingredientes`, datos);
  }

  /** El vendedor confirma que entregó el pedido a la transportadora. */
  marcarEnviado(pedidoId: number): Observable<void> {
    return this.http.post<void>(`/api/vendedor/ventas/${pedidoId}/enviado`, {});
  }

  /** Pedidos pagados con productos de este vendedor y sus cifras. */
  ventas(): Observable<VentasVendedor> {
    return this.http.get<VentasVendedor>('/api/vendedor/ventas');
  }
}

/** Extrae el mensaje de error que envía el backend. */
export function mensajeDeError(e: unknown): string {
  const data = (e as { error?: { message?: string; mensaje?: string; detalles?: string[] } })?.error;
  if (data?.detalles?.length) return data.detalles.join('. ');
  return data?.message ?? data?.mensaje ?? 'Ocurrió un error. Inténtalo de nuevo.';
}

/**
 * Lista larga mostrada por tandas (auditoría R-14): de los ids en orden solo pide al servidor las fichas que se van a
 * ver, y "cargar más" trae la siguiente tanda. Si cambian los ids (otro filtro), vuelve a la primera tanda.
 * Debe crearse en el constructor o en un campo del componente (usa inject y effect).
 */
export function paginarPorIds(ids: Signal<number[]>, alCargar?: (ps: ProductoVendedor[]) => void) {
  const api = inject(ProductosService);
  const cuantos = linkedSignal({ source: ids, computation: () => POR_PAGINA });
  const fichas = signal(new Map<number, ProductoVendedor>());
  const cargando = signal(false);
  const error = signal<string | null>(null);

  effect(() => {
    const faltan = ids().slice(0, cuantos()).filter((id) => !untracked(fichas).has(id));
    if (!faltan.length) return;
    cargando.set(true);
    error.set(null);
    api.porIds(faltan).subscribe({
      next: (ps) => {
        fichas.update((m) => new Map([...m, ...ps.map((p) => [p.id, p] as const)]));
        cargando.set(false);
        alCargar?.(ps);
      },
      error: (e) => {
        error.set(mensajeDeError(e));
        cargando.set(false);
      },
    });
  });

  return {
    visibles: computed(() => ids().slice(0, cuantos()).flatMap((id) => fichas().get(id) ?? [])),
    total: computed(() => ids().length),
    hayMas: computed(() => cuantos() < ids().length),
    cargando: cargando.asReadonly(),
    error: error.asReadonly(),
    cargarMas: () => cuantos.update((n) => n + POR_PAGINA),
  };
}
