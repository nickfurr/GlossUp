import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, panelDeRoles, rutaDePanel } from './auth';

describe('AuthService', () => {
  let http: HttpTestingController;

  function crear(): AuthService {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('al iniciar sesión guarda quién es, pero ningún token (la sesión va en la cookie HttpOnly)', () => {
    const auth = crear();
    let panel = '';
    auth.login('ana@glossup.co', 'clave-segura-1').subscribe((s) => (panel = s.panel));
    http.expectOne('/api/auth/login').flush({ usuarioId: 7, email: 'ana@glossup.co', nombre: 'Ana', roles: ['CLIENTE'] });

    expect(panel).toBe('CLIENTE');
    expect(auth.sesion()?.nombre).toBe('Ana');
    expect(localStorage.getItem('glossup_token')).toBeNull();
    expect(JSON.stringify(localStorage)).not.toContain('token');
  });

  it('una sesión de la versión anterior (token en localStorage) se descarta', () => {
    localStorage.setItem('glossup_token', 'viejo');
    localStorage.setItem('glossup_usuario', JSON.stringify({ email: 'ana@glossup.co', roles: ['CLIENTE'] }));
    const auth = crear();
    expect(auth.sesion()).toBeNull();
    expect(localStorage.getItem('glossup_token')).toBeNull();
  });

  it('salir avisa al servidor para que borre la cookie y olvida al usuario', () => {
    localStorage.setItem('glossup_usuario', JSON.stringify({ usuarioId: 7, email: 'ana@glossup.co', roles: ['CLIENTE'] }));
    const auth = crear();
    expect(auth.sesion()?.email).toBe('ana@glossup.co');

    auth.salir();
    http.expectOne({ method: 'POST', url: '/api/auth/logout' }).flush(null);
    expect(auth.sesion()).toBeNull();
    expect(localStorage.getItem('glossup_usuario')).toBeNull();
  });

  it('cerrar todas las sesiones revoca en el servidor y olvida al usuario', () => {
    localStorage.setItem('glossup_usuario', JSON.stringify({ usuarioId: 7, email: 'ana@glossup.co', roles: ['CLIENTE'] }));
    const auth = crear();
    auth.cerrarTodasLasSesiones().subscribe();
    http.expectOne({ method: 'POST', url: '/api/cuenta/cerrar-sesiones' }).flush(null);
    expect(auth.sesion()).toBeNull();
  });
});

describe('panel según el rol', () => {
  it('el de mayor privilegio manda', () => {
    expect(panelDeRoles(['CLIENTE', 'ADMIN'])).toBe('ADMIN');
    expect(panelDeRoles(['VENDEDOR'])).toBe('VENDEDOR');
    expect(panelDeRoles([])).toBe('CLIENTE');
    expect(rutaDePanel('VENDEDOR')).toBe('/vendedor');
  });
});
