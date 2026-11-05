import { estadoDe } from './compatibilidad';
import type { ResultadoCompatibilidad } from './models';

const r = (parcial: Partial<ResultadoCompatibilidad>) =>
  ({ productoId: 1, compatible: true, recomendado: false, alertas: [], beneficios: [], ...parcial }) as ResultadoCompatibilidad;

describe('estadoDe', () => {
  it('una alerta alta manda sobre todo lo demás', () => {
    expect(estadoDe(r({ compatible: false, recomendado: true }))).toBe('no-compatible');
  });

  it('con alertas leves es precaución, aunque tenga beneficios', () => {
    expect(estadoDe(r({ alertas: [{ severidad: 'BAJA', mensaje: 'm', causa: 'c' }], recomendado: true } as never))).toBe('precaucion');
  });

  it('sin alertas: recomendado si aporta a su perfil, si no compatible', () => {
    expect(estadoDe(r({ recomendado: true }))).toBe('recomendado');
    expect(estadoDe(r({}))).toBe('compatible');
  });
});
