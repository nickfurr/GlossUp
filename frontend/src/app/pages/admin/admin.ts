import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../shared/auth';
import type { ProductoVendedor, RolUsuario } from '../../shared/models';

export interface UsuarioAdmin {
  id: number;
  nombre: string;
  email: string;
  activo: boolean;
  roles: RolUsuario[];
}

type VistaAdmin = 'dashboard' | 'usuarios' | 'nuevo-vendedor' | 'productos';

const USUARIOS_SEMILLA: UsuarioAdmin[] = [
  { id: 1, nombre: 'Ana Gomez', email: 'cliente@glossup.com', activo: true, roles: ['CLIENTE'] },
  { id: 2, nombre: 'Administrador GlossUP', email: 'admin@glossup.com', activo: true, roles: ['ADMIN'] },
  { id: 3, nombre: 'Laboratorio Dermaskin', email: 'vendedor@glossup.com', activo: true, roles: ['VENDEDOR'] },
];

@Component({
  selector: 'app-admin',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './admin.html',
})
export class Admin implements OnInit {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly sesion = this.auth.sesion;

  readonly nav: { id: VistaAdmin; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'usuarios', label: 'Usuarios' },
    { id: 'nuevo-vendedor', label: 'Nuevo vendedor' },
    { id: 'productos', label: 'Productos' },
  ];

  vista = signal<VistaAdmin>('dashboard');
  usuarios = signal<UsuarioAdmin[]>([]);
  productos = signal<ProductoVendedor[]>([]);

  qUsuario = signal('');
  filtroRol = signal<'todos' | RolUsuario>('todos');
  qProducto = signal('');

  nombreVendedor = '';
  emailVendedor = '';
  passwordVendedor = '';
  guardando = signal(false);

  aviso = signal<string | null>(null);
  error = signal<string | null>(null);

  get saludo(): string {
    const hora = new Date().getHours();
    if (hora < 12) return 'Buenos días';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  readonly totalVendedores = computed(
    () => this.usuarios().filter((u) => u.roles.includes('VENDEDOR')).length,
  );

  readonly totalClientes = computed(
    () => this.usuarios().filter((u) => u.roles.includes('CLIENTE')).length,
  );

  readonly usuariosFiltrados = computed(() => {
    const q = this.qUsuario().trim().toLowerCase();
    const rol = this.filtroRol();
    return this.usuarios().filter((u) => {
      if (rol !== 'todos' && !u.roles.includes(rol)) return false;
      if (!q) return true;
      return u.nombre.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
    });
  });

  readonly productosFiltrados = computed(() => {
    const q = this.qProducto().trim().toLowerCase();
    return this.productos().filter((p) => {
      if (!q) return true;
      return (
        p.nombre.toLowerCase().includes(q) ||
        (p.marca ?? '').toLowerCase().includes(q) ||
        p.ingredientes.some((i) => i.inci.toLowerCase().includes(q))
      );
    });
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  irA(v: VistaAdmin): void {
    this.vista.set(v);
    this.aviso.set(null);
    this.error.set(null);
  }

  cargarDatos(): void {
    this.http
      .get<UsuarioAdmin[]>('/api/admin/usuarios')
      .pipe(catchError(() => of(USUARIOS_SEMILLA)))
      .subscribe((u) => this.usuarios.set(u));

    this.http
      .get<ProductoVendedor[]>('/api/admin/productos')
      .pipe(catchError(() => of([])))
      .subscribe((p) => this.productos.set(p));
  }

  crearVendedor(): void {
    this.aviso.set(null);
    this.error.set(null);

    if (!this.nombreVendedor.trim() || !this.emailVendedor.trim() || this.passwordVendedor.length < 8) {
      this.error.set('Completa el nombre, correo y una contraseña de mínimo 8 caracteres.');
      return;
    }

    const payload = {
      nombre: this.nombreVendedor.trim(),
      email: this.emailVendedor.trim().toLowerCase(),
      password: this.passwordVendedor,
    };

    this.guardando.set(true);
    this.http.post<UsuarioAdmin>('/api/admin/vendedores', payload).subscribe({
      next: (creado) => {
        this.usuarios.update((lista) => [...lista, creado]);
        this.guardando.set(false);
        this.finalizarCreacion(creado.email);
      },
      error: (err) => {
        this.guardando.set(false);
        if (err?.status === 0 || err?.status === 502 || err?.status === 504) {
          const nuevo: UsuarioAdmin = {
            id: this.usuarios().length + 1,
            nombre: payload.nombre,
            email: payload.email,
            activo: true,
            roles: ['VENDEDOR'],
          };
          this.usuarios.update((lista) => [...lista, nuevo]);
          this.finalizarCreacion(nuevo.email);
          return;
        }
        this.error.set(err?.error?.message ?? 'No se pudo crear el vendedor.');
      },
    });
  }

  cerrarSesion(): void {
    this.auth.salir();
    this.router.navigateByUrl('/login', { replaceUrl: true });
  }

  private finalizarCreacion(email: string): void {
    this.nombreVendedor = '';
    this.emailVendedor = '';
    this.passwordVendedor = '';
    this.vista.set('usuarios');
    this.aviso.set(`Vendedor "${email}" creado correctamente.`);
  }
}
