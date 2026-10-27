import { Component, computed, input } from '@angular/core';
import { estadoDe } from './compatibilidad';
import type { ResultadoCompatibilidad } from './models';

const ETIQUETA = { recomendado: '★ Recomendado para ti', compatible: '✓ Compatible contigo', precaucion: '! Úsalo con precaución', 'no-compatible': '✕ No compatible contigo' };
const SEVERIDAD = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' };

/** Sello de compatibilidad de un producto con el perfil (HU6) y, al desplegarlo, cada alerta con su causa. */
@Component({
  selector: 'app-compat-info',
  template: `
    @if (resultado(); as r) {
      @if (r.alertas.length) {
        <details class="compat" [class]="'compat compat-' + estado()">
          <summary>{{ etiqueta() }} <span class="compat-n">{{ r.alertas.length }} alerta(s)</span></summary>
          <ul>
            @for (a of r.alertas; track $index) {
              <li [class]="'sev-' + a.severidad">
                <strong>{{ severidad[a.severidad] }}:</strong> {{ a.mensaje }}
                <span class="compat-causa">Por qué: {{ a.causa }}</span>
              </li>
            }
          </ul>
        </details>
      } @else if (r.recomendado) {
        <details class="compat compat-recomendado">
          <summary>{{ etiqueta() }}</summary>
          <ul>
            @for (b of r.beneficios; track $index) { <li>{{ b }}</li> }
          </ul>
        </details>
      } @else {
        <p class="compat compat-compatible">{{ etiqueta() }}</p>
      }
    }
  `,
})
export class CompatInfo {
  readonly resultado = input<ResultadoCompatibilidad | undefined>();
  readonly estado = computed(() => {
    const r = this.resultado();
    return r ? estadoDe(r) : 'compatible';
  });
  readonly etiqueta = computed(() => ETIQUETA[this.estado()]);
  readonly severidad = SEVERIDAD;
}
