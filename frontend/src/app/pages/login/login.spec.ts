import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let componente: Login;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    // jsdom no implementa <dialog>.showModal(); en el navegador abre la ventana del motivo.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) { this.open = true; };
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [Login], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(Login);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function llenarRegistro(rol: 'CLIENTE' | 'VENDEDOR'): void {
    componente.cambiarModo();
    Object.assign(componente, { nombre: 'Tienda', email: 'tienda@glossup.co', password: 'Clave-segura-1',
      password2: 'Clave-segura-1', rol, acepta: true, mayorDeEdad: true });
  }

  it('sin autorizar el tratamiento de datos no envía nada', () => {
    componente.cambiarModo();
    Object.assign(componente, { email: 'a@b.co', password: 'x', password2: 'x', acepta: false, mayorDeEdad: true });
    componente.enviar();
    expect(componente.error()).toContain('autorizar');
    http.expectNone('/api/auth/register');
  });

  it('un vendedor nuevo queda en revisión: no inicia sesión y se le explica', () => {
    llenarRegistro('VENDEDOR');
    componente.enviar();
    const registro = http.expectOne('/api/auth/register');
    expect(registro.request.body).toMatchObject({ rol: 'VENDEDOR', aceptaPolitica: true, mayorDeEdad: true });
    registro.flush(null, { status: 201, statusText: 'Created' });

    http.expectNone('/api/auth/login');
    expect(componente.esRegistro()).toBe(false);
    expect(componente.info()).toContain('administrador la revisará');
    expect(componente.password).toBe('');
  });

  it('un cliente nuevo entra directo a su panel', () => {
    llenarRegistro('CLIENTE');
    componente.enviar();
    http.expectOne('/api/auth/register').flush(null, { status: 201, statusText: 'Created' });
    http.expectOne('/api/auth/login').flush({ usuarioId: 3, email: 'tienda@glossup.co', nombre: 'Tienda', roles: ['CLIENTE'] });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/cliente', { replaceUrl: true });
  });

  it('una cuenta de vendedor en revisión ve el aviso, no la ventana de suspensión', () => {
    Object.assign(componente, { email: 'tienda@glossup.co', password: 'Clave-segura-1' });
    componente.enviar();
    http.expectOne('/api/auth/login').flush(
      { message: 'Tu cuenta de vendedor está en revisión. Te avisaremos por correo cuando un administrador la apruebe.', detalles: [] },
      { status: 403, statusText: 'Forbidden' });
    expect(componente.info()).toContain('en revisión');
    expect(componente.motivoSuspension()).toBeNull();
  });

  it('una cuenta suspendida muestra el motivo del administrador', () => {
    Object.assign(componente, { email: 'ana@glossup.co', password: 'Clave-segura-1' });
    componente.enviar();
    http.expectOne('/api/auth/login').flush({ message: 'Tu cuenta está suspendida', detalles: ['Fraude'] },
      { status: 403, statusText: 'Forbidden' });
    expect(componente.motivoSuspension()).toBe('Fraude');
  });
});
