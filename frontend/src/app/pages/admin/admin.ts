import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../shared/auth';
import { mensajeDeError } from '../../shared/productos';
import { AjustesCuenta } from '../vendedor/ajustes-cuenta';
import type { Pedido, ProductoVendedor, RolUsuario } from '../../shared/models';

export interface UsuarioAdmin {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  roles: RolUsuario[];
  creadoEn?: string;
  motivoSuspension?: string | null;
}

const VISTAS = ['dashboard', 'usuarios', 'productos', 'pedidos', 'cuenta'] as const;
type VistaAdmin = (typeof VISTAS)[number];
type FiltroProductos = 'todos' | 'publicados' | 'bloqueados' | 'baja' | 'sinstock';
const POR_PAGINA = 15;

const ETIQUETA_ROL: Record<RolUsuario, string> = { CLIENTE: 'Cliente', VENDEDOR: 'Vendedor', ADMIN: 'Administrador' };
const ESTADOS_PEDIDO: Record<string, { txt: string; cls: string }> = {
  CREADO: { txt: 'Sin pagar', cls: 'pill-amber' },
  PAGADO: { txt: 'Pagado', cls: 'pill-ok' },
  ENVIADO: { txt: 'Enviado', cls: 'pill-azul' },
  CANCELADO: { txt: 'Cancelado', cls: 'pill-rojo' },
};

/**
 * Panel del ADMIN. Las listas muestran pocas columnas y van por páginas; todo lo que se puede hacer con
 * una cuenta o un producto vive en el panel lateral "Gestionar", así las acciones no se salen de la
 * pantalla. Las reglas (no suspenderse a sí mismo, etc.) las aplica el servidor.
 */
@Component({
  selector: 'app-admin',
  imports: [FormsModule, AjustesCuenta],
  templateUrl: './admin.html',
})
export class Admin {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  private readonly dialogoUsuario = viewChild<ElementRef<HTMLDialogElement>>('dialogoUsuario');
  private readonly dialogoProducto = viewChild<ElementRef<HTMLDialogElement>>('dialogoProducto');
  private readonly dialogoNuevo = viewChild<ElementRef<HTMLDialogElement>>('dialogoNuevo');

  readonly sesion = this.auth.sesion;
  readonly cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  readonly roles: RolUsuario[] = ['CLIENTE', 'VENDEDOR', 'ADMIN'];
  readonly etiquetaRol = ETIQUETA_ROL;
  readonly nav: { id: VistaAdmin; label: string }[] = [
    { id: 'dashboard', label: 'Resumen' },
    { id: 'usuarios', label: 'Usuarios' },
    { id: 'productos', label: 'Productos' },
    { id: 'pedidos', label: 'Pedidos' },
    { id: 'cuenta', label: 'Mi cuenta' },
  ];
  readonly filtrosProducto: { id: FiltroProductos; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'publicados', label: 'Publicados' },
    { id: 'bloqueados', label: 'Suspendidos' },
    { id: 'baja', label: 'Dados de baja' },
    { id: 'sinstock', label: 'Sin stock' },
  ];

  readonly vista = signal<VistaAdmin>(this.vistaDeLaUrl(this.ruta.snapshot.queryParamMap.get('vista')));
  readonly usuarios = signal<UsuarioAdmin[]>([]);
  readonly productos = signal<ProductoVendedor[]>([]);
  readonly pedidos = signal<Pedido[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly qUsuario = signal('');
  readonly filtroRol = signal<'todos' | 'suspendidos' | RolUsuario>('todos');
  readonly paginaUsuarios = signal(0);
  readonly qProducto = signal('');
  readonly filtroProducto = signal<FiltroProductos>('todos');
  readonly paginaProductos = signal(0);
  readonly filtroPedido = signal<string>('todos');
  readonly paginaPedidos = signal(0);

  // ---- Panel "Gestionar" ----
  readonly usuarioActivo = signal<UsuarioAdmin | null>(null);
  readonly productoActivo = signal<ProductoVendedor | null>(null);
  readonly ocupado = signal(false);
  readonly errorGestion = signal<string | null>(null);
  readonly avisoGestion = signal<string | null>(null);
  readonly enlace = signal<string | null>(null);
  readonly copiado = signal(false);
  edNombre = '';
  edEmail = '';
  edRol: RolUsuario = 'CLIENTE';
  edMotivo = '';
  nuevoNombre = '';
  nuevoEmail = '';
  nuevoClave = '';

  readonly saludo = computed(() => {
    const hora = new Date().getHours();
    return hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  });
  readonly esMiCuenta = computed(() => this.usuarioActivo()?.id === this.sesion()?.usuarioId);
  readonly suspendidos = computed(() => this.usuarios().filter((u) => !u.activo).length);
  readonly vendedores = computed(() => this.usuarios().filter((u) => u.roles.includes('VENDEDOR')).length);
  readonly clientes = computed(() => this.usuarios().filter((u) => u.roles.includes('CLIENTE')).length);
  readonly publicados = computed(() => this.productos().filter((p) => p.activo && !p.baneado).length);
  readonly bloqueados = computed(() => this.productos().filter((p) => p.baneado).length);
  readonly porPagar = computed(() => this.pedidos().filter((p) => p.estado === 'CREADO').length);
  readonly porEnviar = computed(() => this.pedidos().filter((p) => p.estado === 'PAGADO').length);
  readonly ingresos = computed(() => this.pedidos()
    .filter((p) => p.estado === 'PAGADO' || p.estado === 'ENVIADO')
    .reduce((n, p) => n + p.total, 0));
  readonly ultimosUsuarios = computed(() => [...this.usuarios()].sort((a, b) => b.id - a.id).slice(0, 5));
  readonly ultimosPedidos = computed(() => this.pedidos().slice(0, 5));
  private readonly nombres = computed(() => new Map(this.usuarios().map((u) => [u.id, u.nombre])));

  readonly usuariosFiltrados = computed(() => {
    const q = this.qUsuario().trim().toLowerCase();
    const f = this.filtroRol();
    return this.usuarios().filter((u) => {
      if (f === 'suspendidos' && u.activo) return false;
      if (f !== 'todos' && f !== 'suspendidos' && !u.roles.includes(f)) return false;
      return !q || u.nombre.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
    });
  });
  readonly productosFiltrados = computed(() => {
    const q = this.qProducto().trim().toLowerCase();
    const f = this.filtroProducto();
    return this.productos().filter((p) => {
      if (f === 'publicados' && (!p.activo || p.baneado)) return false;
      if (f === 'bloqueados' && !p.baneado) return false;
      if (f === 'baja' && p.activo) return false;
      if (f === 'sinstock' && p.stock > 0) return false;
      return !q || p.nombre.toLowerCase().includes(q) || (p.marca ?? '').toLowerCase().includes(q)
        || p.ingredientes.some((i) => i.inci.toLowerCase().includes(q));
    });
  });
  readonly pedidosFiltrados = computed(() => {
    const f = this.filtroPedido();
    return this.pedidos().filter((p) => f === 'todos' || p.estado === f);
  });
  readonly usuariosPagina = computed(() => this.pagina(this.usuariosFiltrados(), this.paginaUsuarios()));
  readonly productosPagina = computed(() => this.pagina(this.productosFiltrados(), this.paginaProductos()));
  readonly pedidosPagina = computed(() => this.pagina(this.pedidosFiltrados(), this.paginaPedidos()));

  constructor() {
    this.ruta.queryParamMap.pipe(takeUntilDestroyed()).subscribe((p) => this.vista.set(this.vistaDeLaUrl(p.get('vista'))));
    this.cargar();
  }

  private vistaDeLaUrl(valor: string | null): VistaAdmin {
    return VISTAS.includes(valor as VistaAdmin) ? (valor as VistaAdmin) : 'dashboard';
  }

  private pagina<T>(lista: T[], pagina: number): T[] {
    const inicio = Math.min(pagina, this.ultimaPagina(lista.length)) * POR_PAGINA;
    return lista.slice(inicio, inicio + POR_PAGINA);
  }

  ultimaPagina(total: number): number {
    return Math.max(0, Math.ceil(total / POR_PAGINA) - 1);
  }

  /** Texto del paginador; una búsqueda puede dejar la página actual más allá del final. */
  rango(total: number, pagina: number): string {
    if (total === 0) return '0 resultados';
    const p = Math.min(pagina, this.ultimaPagina(total));
    return `${p * POR_PAGINA + 1}–${Math.min((p + 1) * POR_PAGINA, total)} de ${total}`;
  }

  irA(vista: VistaAdmin): void {
    this.vista.set(vista);
    this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista } });
  }

  verUsuarios(filtro: 'todos' | 'suspendidos' | RolUsuario): void {
    this.filtroRol.set(filtro);
    this.qUsuario.set('');
    this.paginaUsuarios.set(0);
    this.irA('usuarios');
  }

  verProductos(filtro: FiltroProductos): void {
    this.filtroProducto.set(filtro);
    this.qProducto.set('');
    this.paginaProductos.set(0);
    this.irA('productos');
  }

  verPedidos(filtro: string): void {
    this.filtroPedido.set(filtro);
    this.paginaPedidos.set(0);
    this.irA('pedidos');
  }

  cargar(): void {
    this.error.set(null);
    let pendientes = 3;
    const fin = () => {
      if (--pendientes === 0) this.cargando.set(false);
    };
    const fallo = (e: unknown) => {
      this.error.set(mensajeDeError(e));
      fin();
    };
    this.http.get<UsuarioAdmin[]>('/api/admin/usuarios').subscribe({ next: (u) => { this.usuarios.set(u); fin(); }, error: fallo });
    this.http.get<ProductoVendedor[]>('/api/admin/productos').subscribe({ next: (p) => { this.productos.set(p); fin(); }, error: fallo });
    this.http.get<Pedido[]>('/api/admin/pedidos').subscribe({ next: (p) => { this.pedidos.set(p); fin(); }, error: fallo });
  }

  cerrarSesion(): void {
    this.auth.salir();
    this.router.navigateByUrl('/login', { replaceUrl: true });
  }

  nombreDe(id: number | null | undefined): string {
    return (id != null && this.nombres().get(id)) || 'Sin datos';
  }

  estadoPedido(p: Pedido): { txt: string; cls: string } {
    const etapa = p.envio?.estado;
    if (p.estado !== 'CANCELADO' && etapa === 'ENTREGADO') return { txt: 'Entregado', cls: 'pill-ok' };
    if (p.estado !== 'CANCELADO' && etapa === 'NO_ENTREGADO') return { txt: 'No entregado', cls: 'pill-rojo' };
    return ESTADOS_PEDIDO[p.estado] ?? { txt: p.estado, cls: '' };
  }

  estadoProducto(p: ProductoVendedor): { txt: string; cls: string } {
    if (p.baneado) return { txt: 'Suspendido', cls: 'rojo' };
    if (!p.activo) return { txt: 'Dado de baja', cls: 'amber' };
    return p.stock === 0 ? { txt: 'Sin stock', cls: 'amber' } : { txt: 'Publicado', cls: 'ok' };
  }

  fecha(iso?: string | null): string {
    return iso ? new Date(iso).toLocaleDateString('es-CO', { dateStyle: 'medium' }) : '—';
  }

  // ---- Gestionar usuario ----
  private limpiarGestion(): void {
    this.errorGestion.set(null);
    this.avisoGestion.set(null);
    this.enlace.set(null);
    this.copiado.set(false);
    this.edMotivo = '';
  }

  abrirUsuario(u: UsuarioAdmin): void {
    this.limpiarGestion();
    this.usuarioActivo.set(u);
    this.edNombre = u.nombre;
    this.edEmail = u.email;
    this.edRol = u.roles[0] ?? 'CLIENTE';
    this.dialogoUsuario()?.nativeElement.showModal();
  }

  /** Una acción del panel: muestra su resultado sin cerrarlo, y deja la lista al día. */
  private accionUsuario(peticion: Observable<UsuarioAdmin>, exito: string, despues?: (u: UsuarioAdmin) => void): void {
    this.errorGestion.set(null);
    this.avisoGestion.set(null);
    this.ocupado.set(true);
    peticion.subscribe({
      next: (u) => {
        this.usuarioActivo.set(u);
        this.usuarios.update((lista) => lista.map((x) => (x.id === u.id ? u : x)));
        this.edMotivo = '';
        this.avisoGestion.set(exito);
        this.ocupado.set(false);
        despues?.(u);
      },
      error: (e) => {
        this.errorGestion.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  guardarDatos(u: UsuarioAdmin): void {
    this.accionUsuario(
      this.http.put<UsuarioAdmin>(`/api/admin/usuarios/${u.id}`, { nombre: this.edNombre.trim(), email: this.edEmail.trim() }),
      'Guardamos los datos.',
    );
  }

  guardarRol(u: UsuarioAdmin): void {
    this.accionUsuario(this.http.put<UsuarioAdmin>(`/api/admin/usuarios/${u.id}/rol`, { rol: this.edRol }),
      'Cambiamos el rol. Tendrá que volver a iniciar sesión.');
  }

  suspender(u: UsuarioAdmin): void {
    if (!this.edMotivo.trim()) {
      this.errorGestion.set('Escribe el motivo de la suspensión.');
      return;
    }
    this.accionUsuario(this.http.post<UsuarioAdmin>(`/api/admin/usuarios/${u.id}/suspender`, { motivo: this.edMotivo.trim() }),
      'Cuenta suspendida. Sus sesiones abiertas ya no funcionan.', () => this.refrescarCatalogoSiVende(u));
  }

  reactivar(u: UsuarioAdmin): void {
    this.accionUsuario(this.http.post<UsuarioAdmin>(`/api/admin/usuarios/${u.id}/reactivar`, {}), 'Cuenta reactivada.', () => this.refrescarCatalogoSiVende(u));
  }

  /** Suspender o reactivar a un vendedor bloquea o devuelve sus productos en el servidor. */
  private refrescarCatalogoSiVende(u: UsuarioAdmin): void {
    if (u.roles.includes('VENDEDOR')) {
      this.http.get<ProductoVendedor[]>('/api/admin/productos').subscribe((p) => this.productos.set(p));
    }
  }

  generarEnlace(u: UsuarioAdmin): void {
    this.errorGestion.set(null);
    this.copiado.set(false);
    this.ocupado.set(true);
    this.http.post<{ enlace: string }>(`/api/admin/usuarios/${u.id}/recuperacion`, {}).subscribe({
      next: ({ enlace }) => {
        this.enlace.set(enlace);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.errorGestion.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  copiarEnlace(): void {
    const enlace = this.enlace();
    if (enlace) navigator.clipboard?.writeText(enlace).then(() => this.copiado.set(true));
  }

  // ---- Gestionar producto ----
  abrirProducto(p: ProductoVendedor): void {
    this.limpiarGestion();
    this.productoActivo.set(p);
    this.dialogoProducto()?.nativeElement.showModal();
  }

  private accionProducto(peticion: Observable<ProductoVendedor>, exito: string): void {
    this.errorGestion.set(null);
    this.avisoGestion.set(null);
    this.ocupado.set(true);
    peticion.subscribe({
      next: (p) => {
        this.productoActivo.set(p);
        this.productos.update((lista) => lista.map((x) => (x.id === p.id ? p : x)));
        this.edMotivo = '';
        this.avisoGestion.set(exito);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.errorGestion.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }

  bloquear(p: ProductoVendedor): void {
    if (!this.edMotivo.trim()) {
      this.errorGestion.set('Escribe el motivo de la suspensión.');
      return;
    }
    this.accionProducto(this.http.post<ProductoVendedor>(`/api/admin/productos/${p.id}/banear`, { motivo: this.edMotivo.trim() }),
      'Producto suspendido. Ya no aparece en el catálogo.');
  }

  desbloquear(p: ProductoVendedor): void {
    this.accionProducto(this.http.post<ProductoVendedor>(`/api/admin/productos/${p.id}/desbanear`, {}), 'Suspensión levantada.');
  }

  // ---- Nuevo vendedor ----
  abrirNuevo(): void {
    this.limpiarGestion();
    this.nuevoNombre = '';
    this.nuevoEmail = '';
    this.nuevoClave = '';
    this.dialogoNuevo()?.nativeElement.showModal();
  }

  crearVendedor(): void {
    this.errorGestion.set(null);
    if (!this.nuevoNombre.trim() || !this.nuevoEmail.trim() || this.nuevoClave.length < 8) {
      this.errorGestion.set('Completa el nombre, el correo y una contraseña de mínimo 8 caracteres.');
      return;
    }
    this.ocupado.set(true);
    this.http.post<UsuarioAdmin>('/api/admin/vendedores', {
      nombre: this.nuevoNombre.trim(), email: this.nuevoEmail.trim().toLowerCase(), password: this.nuevoClave,
    }).subscribe({
      next: (creado) => {
        this.usuarios.update((lista) => [...lista, creado]);
        this.ocupado.set(false);
        this.nuevoClave = '';
        this.dialogoNuevo()?.nativeElement.close();
        this.verUsuarios('VENDEDOR');
      },
      error: (e) => {
        this.errorGestion.set(mensajeDeError(e));
        this.ocupado.set(false);
      },
    });
  }
}
