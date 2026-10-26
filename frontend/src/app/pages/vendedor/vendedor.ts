import { Component, computed, inject, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/auth';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import type { DatosProducto, IngredienteCatalogo, ProductoVendedor } from '../../shared/models';

type Vista = 'dashboard' | 'productos' | 'editor' | 'pedidos' | 'ventas' | 'ajustes';
interface InciLocal {
  inci: string;
  descripcion: string;
  concentracion: string;
}

/**
 * Panel del VENDEDOR (mockups 15-17): barra lateral + Dashboard, Mis productos (tabla) y editor a
 * dos columnas con la ficha INCI. Crear/editar envían el producto completo (datos + imágenes +
 * ficha, mínimo un ingrediente); eliminar es borrado lógico.
 */
@Component({
  selector: 'app-vendedor',
  imports: [FormsModule],
  templateUrl: './vendedor.html',
})
export class Vendedor {
  private auth = inject(AuthService);
  private api = inject(ProductosService);
  private router = inject(Router);

  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly sesion = this.auth.sesion;
  readonly productos = signal<ProductoVendedor[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly vista = signal<Vista>('dashboard');

  // Mis productos: buscador y filtros
  readonly q = signal('');
  readonly filtro = signal<'todos' | 'activos' | 'sinstock'>('todos');

  readonly nav: { id: Vista; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'productos', label: 'Mis productos' },
    { id: 'editor', label: 'Nuevo producto' },
    { id: 'pedidos', label: 'Pedidos' },
    { id: 'ventas', label: 'Ventas' },
    { id: 'ajustes', label: 'Ajustes' },
  ];

  readonly tituloVista = computed(() => this.nav.find((n) => n.id === this.vista())?.label ?? '');
  readonly activos = computed(() => this.productos().filter((p) => p.activo).length);
  readonly alertas = computed(() => this.productos().filter((p) => p.ingredientes.length === 0).length);

  readonly lista = computed(() => {
    const texto = this.q().trim().toLowerCase();
    const f = this.filtro();
    return this.productos().filter((p) => {
      if (f === 'activos' && !p.activo) return false;
      if (f === 'sinstock' && p.stock !== 0) return false;
      if (!texto) return true;
      return (
        p.nombre.toLowerCase().includes(texto) ||
        p.ingredientes.some((i) => i.inci.toLowerCase().includes(texto))
      );
    });
  });

  // ---- Estado del editor ----
  readonly edEsNuevo = signal(true);
  private edId = 0;
  edNombre = '';
  edCategoria = '';
  edPrecio = '';
  edStock = '0';
  edInvima = '';
  readonly edImagenes = signal<string[]>(['']);
  readonly edFicha = signal<InciLocal[]>([]);
  edInci = '';
  edConc = '';
  /** Ingrediente elegido de la lista: solo se puede agregar a la ficha lo que viene del catálogo. */
  edElegido: IngredienteCatalogo | null = null;
  readonly sugerencias = signal<IngredienteCatalogo[]>([]);
  private busquedaInci = new Subject<string>();
  readonly edError = signal<string | null>(null);
  readonly edOcupado = signal(false);

  constructor() {
    this.busquedaInci
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) => (q.trim().length < 2 ? of([]) : this.api.buscarIngredientes(q.trim()))),
      )
      .subscribe({ next: (r) => this.sugerencias.set(r), error: () => this.sugerencias.set([]) });
    this.api.listarMisProductos().subscribe({
      next: (ps) => {
        this.productos.set(ps);
        this.cargando.set(false);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
      },
    });
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  estadoDe(p: ProductoVendedor): { txt: string; cls: string } {
    if (!p.activo) return { txt: 'Inactivo', cls: 'amber' };
    if (p.stock === 0) return { txt: 'Sin stock', cls: 'rojo' };
    return { txt: 'Indexado', cls: 'ok' };
  }

  irA(id: Vista): void {
    if (id === 'editor') this.abrirNuevo();
    else this.vista.set(id);
  }

  cerrarSesion(): void {
    this.auth.salir();
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  private aplicar(p: ProductoVendedor): void {
    this.productos.update((prev) => {
      const i = prev.findIndex((x) => x.id === p.id);
      if (i === -1) return [p, ...prev];
      const copia = [...prev];
      copia[i] = p;
      return copia;
    });
  }

  // ---- Abrir editor ----
  abrirNuevo(): void {
    this.edEsNuevo.set(true);
    this.edId = 0;
    this.edNombre = '';
    this.edCategoria = '';
    this.edPrecio = '';
    this.edStock = '0';
    this.edInvima = '';
    this.edImagenes.set(['']);
    this.edFicha.set([]);
    this.limpiarAddInci();
    this.edError.set(null);
    this.vista.set('editor');
  }

  abrirEditor(p: ProductoVendedor): void {
    this.edEsNuevo.set(false);
    this.edId = p.id;
    this.edNombre = p.nombre;
    this.edCategoria = p.categoria ?? '';
    this.edPrecio = String(p.precio ?? '');
    this.edStock = String(p.stock ?? '0');
    this.edInvima = p.registroInvima ?? '';
    this.edImagenes.set(p.imagenes?.length ? [...p.imagenes] : ['']);
    this.edFicha.set(
      (p.ingredientes ?? []).map((i) => ({
        inci: i.inci,
        descripcion: i.descripcion ?? '',
        concentracion: i.concentracion != null ? String(i.concentracion) : '',
      })),
    );
    this.limpiarAddInci();
    this.edError.set(null);
    this.vista.set('editor');
  }

  private limpiarAddInci(): void {
    this.edInci = '';
    this.edConc = '';
    this.edElegido = null;
    this.sugerencias.set([]);
  }

  // ---- Imágenes ----
  cambiarImagen(idx: number, valor: string): void {
    this.edImagenes.update((imgs) => imgs.map((u, i) => (i === idx ? valor : u)));
  }
  agregarImagen(): void {
    this.edImagenes.update((imgs) => [...imgs, '']);
  }
  quitarImagen(idx: number): void {
    this.edImagenes.update((imgs) => imgs.filter((_, i) => i !== idx));
  }
  get primeraImagen(): string | undefined {
    return this.edImagenes().find((u) => u.trim());
  }

  // ---- Ficha INCI ----
  buscarInci(texto: string): void {
    this.edElegido = null; // si vuelve a escribir, debe volver a elegir de la lista
    this.busquedaInci.next(texto);
  }
  elegirInci(i: IngredienteCatalogo): void {
    this.edElegido = i;
    this.edInci = i.inci;
    this.sugerencias.set([]);
  }
  agregarInci(): void {
    const elegido = this.edElegido;
    if (!elegido) return;
    if (this.edFicha().some((x) => x.inci === elegido.inci)) return;
    this.edFicha.update((f) => [...f, { inci: elegido.inci, descripcion: elegido.descripcion ?? '', concentracion: this.edConc }]);
    this.limpiarAddInci();
  }
  quitarInci(nombreInci: string): void {
    this.edFicha.update((f) => f.filter((x) => x.inci !== nombreInci));
  }

  private datos(): DatosProducto {
    return {
      nombre: this.edNombre.trim(),
      categoria: this.edCategoria.trim() || undefined,
      precio: Number(this.edPrecio),
      stock: Number(this.edStock),
      ph: null,
      registroInvima: this.edInvima.trim() || undefined,
      imagenes: this.edImagenes().map((u) => u.trim()).filter(Boolean),
      ingredientes: this.edFicha().map((f) => ({
        inci: f.inci,
        concentracion: f.concentracion ? Number(f.concentracion) : null,
      })),
    };
  }

  guardar(): void {
    if (this.edFicha().length === 0) {
      this.edError.set('La ficha INCI debe tener al menos un ingrediente para poder guardar.');
      return;
    }
    this.edError.set(null);
    this.edOcupado.set(true);
    const peticion = this.edEsNuevo() ? this.api.crear(this.datos()) : this.api.actualizar(this.edId, this.datos());
    peticion.subscribe({
      next: (p) => {
        this.aplicar(p);
        this.edOcupado.set(false);
        this.vista.set('productos');
      },
      error: (e) => {
        this.edError.set(mensajeDeError(e));
        this.edOcupado.set(false);
      },
    });
  }

  eliminar(): void {
    if (this.edEsNuevo()) return;
    const id = this.edId;
    this.edError.set(null);
    this.edOcupado.set(true);
    this.api.eliminar(id).subscribe({
      next: () => {
        const actual = this.productos().find((x) => x.id === id);
        if (actual) this.aplicar({ ...actual, activo: false });
        this.edOcupado.set(false);
        this.vista.set('productos');
      },
      error: (e) => {
        this.edError.set(mensajeDeError(e));
        this.edOcupado.set(false);
      },
    });
  }
}
