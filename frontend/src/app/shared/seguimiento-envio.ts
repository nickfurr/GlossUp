import { Component, computed, input } from '@angular/core';
import { fechaLlegada } from './direcciones';
import type { Pedido } from './models';

/** Seguimiento del envío de un pedido (HU14): etapas con su fecha y a dónde va. */
@Component({
  selector: 'app-seguimiento-envio',
  template: `
    @if (pedido().envio; as e) {
      <div class="envio">
        @if (pedido().estado !== 'CREADO') {
          <ol class="etapas" aria-label="Seguimiento del envío">
            @for (et of etapas(); track et.texto; let i = $index) {
              <li [class.hecha]="et.fecha" [class.alerta]="et.alerta && et.fecha" [attr.aria-current]="i === actual() ? 'step' : null">
                <strong>{{ et.texto }}</strong>
                @if (et.fecha) { <span>{{ fecha(et.fecha) }}</span> }
              </li>
            }
          </ol>
        }
        <dl>
          @if (e.estado !== 'ENTREGADO' && e.estado !== 'NO_ENTREGADO') {
            <div><dt>Llegada estimada</dt><dd class="llegada">{{ llegada(e.fechaEstimada) }}</dd></div>
          }
          <div><dt>Enviar a</dt><dd>{{ e.direccion }}{{ e.complemento ? ', ' + e.complemento : '' }}, {{ e.barrio }}, {{ e.ciudad }}, {{ e.departamento }}</dd></div>
          <div><dt>Recibe</dt><dd>{{ e.nombreRecibe }} · {{ e.telefono }}</dd></div>
          @if (e.guia) { <div><dt>Guía {{ e.transportadora }}</dt><dd>{{ e.guia }}</dd></div> }
        </dl>
      </div>
    }
  `,
  styles: [`
    .envio { display: grid; gap: 1rem; margin-top: 1rem; }
    .etapas { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(4, 1fr); }
    li { position: relative; padding: 1.3rem .5rem 0 0; display: grid; gap: .1rem; align-content: start; color: #8a7681; font-size: .85rem; }
    li::before { content: ''; position: absolute; top: .3rem; left: 0; width: .8rem; height: .8rem; border-radius: 50%; background: var(--borde); }
    li::after { content: ''; position: absolute; top: .6rem; left: .8rem; right: 0; height: 2px; background: var(--borde); }
    li:last-child::after { display: none; }
    li.hecha { color: var(--tinta); }
    li.hecha::before, li.hecha::after { background: var(--verde-ok); }
    li.alerta::before { background: #9b1c36; }
    li[aria-current]::after { background: var(--borde); }
    li span { font-size: .76rem; color: #7c6870; }
    dl { display: grid; grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr)); gap: .7rem 1.4rem; margin: 0; padding: .9rem 1rem; border-radius: 12px; background: var(--crema); }
    dt { color: #7c6870; font-size: .75rem; }
    dd { margin: 0; font-weight: 700; overflow-wrap: anywhere; }
    .llegada { color: var(--verde-ok); }
    .llegada::first-letter { text-transform: uppercase; }
    @media (max-width: 640px) {
      .etapas { grid-template-columns: 1fr; gap: .1rem; }
      li { padding: 0 0 .7rem 1.4rem; }
      li::after { top: 1.1rem; bottom: 0; left: .35rem; right: auto; width: 2px; height: auto; }
    }
  `],
})
export class SeguimientoEnvio {
  readonly pedido = input.required<Pedido>();
  readonly llegada = fechaLlegada;

  readonly etapas = computed(() => {
    const p = this.pedido();
    const e = p.envio!;
    const noEntregado = e.estado === 'NO_ENTREGADO';
    return [
      { texto: 'Pago aprobado', fecha: p.pagadoEn, alerta: false },
      { texto: 'Listo para envío', fecha: e.notificadoEn, alerta: false },
      { texto: 'En camino', fecha: e.enviadoEn, alerta: false },
      noEntregado
        ? { texto: 'No se pudo entregar', fecha: e.noEntregadoEn, alerta: true }
        : { texto: 'Entregado', fecha: e.entregadoEn, alerta: false },
    ];
  });

  readonly actual = computed(() => this.etapas().map((et) => !!et.fecha).lastIndexOf(true));

  fecha(iso?: string | null): string {
    return iso ? new Date(iso).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '';
  }
}
