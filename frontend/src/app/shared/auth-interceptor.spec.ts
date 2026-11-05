import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth-interceptor';
import { AuthService } from './auth';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    localStorage.setItem('glossup_usuario', JSON.stringify({ usuarioId: 7, email: 'ana@glossup.co', roles: ['CLIENTE'] }));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('no agrega encabezado Authorization: la cookie viaja sola', () => {
    http.get('/api/perfil/me').subscribe();
    const req = backend.expectOne('/api/perfil/me');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('un 401 de la API (sesión vencida o cerrada en otro dispositivo) olvida al usuario y manda a /login', () => {
    http.get('/api/carrito').subscribe({ error: () => {} });
    backend.expectOne('/api/carrito').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sesion()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('un 401 del login es "clave incorrecta": lo muestra el formulario, no cierra nada', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sesion()).not.toBeNull();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
