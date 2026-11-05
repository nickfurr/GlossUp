import { DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

/** Segundos que pide esperar un 429 (Retry-After), o null si el error no es un 429. */
export function segundosDeEspera(e: unknown): number | null {
  const err = e as HttpErrorResponse;
  if (err?.status !== 429) return null;
  const header = Number(err.headers?.get('Retry-After'));
  return Number.isFinite(header) && header > 0 ? Math.ceil(header) : 60; // respaldo si el header no llega
}

/** Formatea 75 -> "1 min 15 s", 42 -> "42 s". */
export function formatoEspera(seg: number): string {
  if (seg < 60) return `${seg} s`;
  const min = Math.floor(seg / 60);
  const s = seg % 60;
  return s > 0 ? `${min} min ${s} s` : `${min} min`;
}

/**
 * Cuenta regresiva ligada al ciclo de vida del componente. Se crea en el contexto de inyección
 * (como campo del componente) y limpia su intervalo al destruirse.
 */
export class EsperaReintento {
  private readonly restante = signal(0);
  readonly bloqueado = computed(() => this.restante() > 0);
  readonly mensaje = computed(() =>
    this.bloqueado() ? `Demasiados intentos. Podrás reintentar en ${formatoEspera(this.restante())}.` : null);
  private timer?: ReturnType<typeof setInterval>;

  constructor() {
    try {
      inject(DestroyRef).onDestroy(() => this.detener());
    } catch {
      // Por si se instancia fuera de contexto de inyección
    }
  }

  /** Si el error es un 429, arranca la cuenta y devuelve true. */
  manejar(e: unknown): boolean {
    const seg = segundosDeEspera(e);
    if (seg === null) return false;
    this.iniciar(seg);
    return true;
  }

  private iniciar(seg: number): void {
    this.detener();
    const fin = Date.now() + seg * 1000;
    const tick = () => {
      const r = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
      this.restante.set(r);
      if (r === 0) this.detener();
    };
    tick();
    this.timer = setInterval(tick, 1000);
  }

  private detener(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }
}
