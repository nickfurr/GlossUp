import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductosService, mensajeDeError } from './productos';

describe('ProductosService', () => {
  let api: ProductosService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ProductosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide las fichas por ids y devuelve solo los productos', () => {
    let nombres: string[] = [];
    api.porIds([3, 1]).subscribe((ps) => (nombres = ps.map((p) => p.nombre)));
    const req = http.expectOne((r) => r.url === '/api/catalogo/productos');
    expect(req.request.params.get('ids')).toBe('3,1');
    req.flush({ items: [{ id: 3, nombre: 'C' }, { id: 1, nombre: 'A' }], total: 2 });
    expect(nombres).toEqual(['C', 'A']);
  });

  it('sin ids no llama al servidor', () => {
    let vacio: unknown[] | undefined;
    api.porIds([]).subscribe((ps) => (vacio = ps));
    expect(vacio).toEqual([]);
  });

  it('las recomendaciones traen la ficha de cada producto con su razón', () => {
    let resultado: { perfilVacio: boolean; productos: { id: number; razon: string }[] } | undefined;
    api.recomendados(2).subscribe((r) => (resultado = r));
    const rec = http.expectOne((r) => r.url === '/api/recomendaciones');
    expect(rec.request.params.get('limite')).toBe('2');
    rec.flush({ perfilVacio: false, recomendaciones: [{ productoId: 9, razon: 'Contiene niacinamide' }] });
    http.expectOne((r) => r.url === '/api/catalogo/productos' && r.params.get('ids') === '9')
      .flush({ items: [{ id: 9, nombre: 'Sérum' }], total: 1 });
    expect(resultado?.productos[0]).toMatchObject({ id: 9, razon: 'Contiene niacinamide' });
  });
});

describe('mensajeDeError', () => {
  it('prefiere los detalles de validación, luego el mensaje, y si no hay nada uno genérico', () => {
    expect(mensajeDeError({ error: { message: 'x', detalles: ['nombre: obligatorio', 'precio: positivo'] } }))
      .toBe('nombre: obligatorio. precio: positivo');
    expect(mensajeDeError({ error: { message: 'Stock insuficiente' } })).toBe('Stock insuficiente');
    expect(mensajeDeError(new Error('red'))).toBe('Ocurrió un error. Inténtalo de nuevo.');
  });
});
