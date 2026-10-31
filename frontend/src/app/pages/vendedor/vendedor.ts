import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../shared/auth';
import { ProductosService, mensajeDeError } from '../../shared/productos';
import { SeguimientoEnvio } from '../../shared/seguimiento-envio';
import { AjustesCuenta } from './ajustes-cuenta';
import type { DatosProducto, IngredienteCatalogo, ProductoVendedor, ResumenVentas, VentaVendedor } from '../../shared/models';

const VISTAS = ['dashboard', 'productos', 'editor', 'pedidos', 'ventas', 'ajustes'] as const;
type Vista = (typeof VISTAS)[number];
type FiltroProductos = 'todos' | 'activos' | 'sinstock' | 'pocas' | 'alertas' | 'baja';
type FiltroPedidos = 'todos' | 'porEnviar' | 'enCamino' | 'finalizados';
const STOCK_BAJO = 5;
const MAX_FOTO_BYTES = 8 * 1024 * 1024;
interface InciLocal {
  inci: string;
  descripcion: string;
  concentracion: string;
}

/**
 * Panel del VENDEDOR (mockups 15-17): barra lateral + Dashboard, Mis productos, editor a dos columnas
 * con la ficha INCI, Pedidos, Ventas y Ajustes. Crear/editar envían el producto completo; dar de baja
 * es borrado lógico y se puede revertir. La vista va en la URL para que funcione el botón Atrás.
 */
@Component({
  selector: 'app-vendedor',
  imports: [FormsModule, SeguimientoEnvio, AjustesCuenta],
  templateUrl: './vendedor.html',
})
export class Vendedor {
  private auth = inject(AuthService);
  private api = inject(ProductosService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);

  private cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  readonly sesion = this.auth.sesion;
  readonly productos = signal<ProductoVendedor[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly vista = signal<Vista>(this.vistaDeLaUrl(this.ruta.snapshot.queryParamMap.get('vista')));
  readonly resumen = signal<ResumenVentas | null>(null);
  readonly ventas = signal<VentaVendedor[]>([]);
  readonly cargandoVentas = signal(true);
  readonly errorVentas = signal<string | null>(null);
  readonly filtroPedidos = signal<FiltroPedidos>('todos');
  readonly ventaAbierta = signal<number | null>(null);

  // Mis productos: buscador y filtros
  readonly q = signal('');
  readonly filtro = signal<FiltroProductos>('todos');

  readonly nav: { id: Vista; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'productos', label: 'Mis productos' },
    { id: 'editor', label: 'Nuevo producto' },
    { id: 'pedidos', label: 'Pedidos' },
    { id: 'ventas', label: 'Ventas' },
    { id: 'ajustes', label: 'Ajustes' },
  ];

  readonly saludo = computed(() => {
    const hora = new Date().getHours();
    return hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  });
  readonly activos = computed(() => this.productos().filter((p) => p.activo).length);
  readonly porReabastecer = computed(() => this.productos().filter((p) => this.stockBajo(p)).length);
  readonly alertas = computed(() => this.productos().filter((p) => this.faltantes(p).length > 0).length);
  readonly semanas = computed(() => {
    const semanas = this.resumen()?.semanas ?? [];
    const mayor = Math.max(0, ...semanas.map((s) => s.ingresos));
    return semanas.map((s) => ({
      ...s,
      etiqueta: new Date(`${s.inicio}T12:00:00`).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' }),
      // Corta para que las 8 semanas quepan sin pisarse en un celular.
      eje: new Date(`${s.inicio}T12:00:00`).toLocaleDateString('es-CO', { day: 'numeric', month: 'numeric' }),
      alto: mayor ? (s.ingresos / mayor) * 100 : 0,
      esMayor: mayor > 0 && s.ingresos === mayor,
    }));
  });
  readonly hayVentasRecientes = computed(() => this.semanas().some((s) => s.ingresos > 0));
  readonly descripcionGrafica = computed(() =>
    'Ventas por semana: ' + this.semanas().map((s) => `semana del ${s.etiqueta}, ${this.formatearCop(s.ingresos)}`).join('; '));
  readonly pedidosFiltrados = computed(() => {
    const f = this.filtroPedidos();
    return this.ventas().filter((v) => f === 'todos' || this.grupoDe(v) === f);
  });
  readonly porEnviar = computed(() => this.ventas().filter((v) => this.grupoDe(v) === 'porEnviar').length);

  readonly lista = computed(() => {
    const texto = this.q().trim().toLowerCase();
    const f = this.filtro();
    return this.productos().filter((p) => {
      if (f === 'activos' && !p.activo) return false;
      if (f === 'sinstock' && (!p.activo || p.stock !== 0)) return false;
      if (f === 'pocas' && !this.stockBajo(p)) return false;
      if (f === 'alertas' && this.faltantes(p).length === 0) return false;
      if (f === 'baja' && p.activo) return false;
      if (!texto) return true;
      return (
        p.nombre.toLowerCase().includes(texto) ||
        p.ingredientes.some((i) => i.inci.toLowerCase().includes(texto))
      );
    });
  });

  readonly filtrosProductos: { id: FiltroProductos; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'activos', label: 'Activos' },
    { id: 'sinstock', label: 'Sin stock' },
    { id: 'pocas', label: 'Por reabastecer' },
    { id: 'alertas', label: 'Por completar' },
    { id: 'baja', label: 'Dados de baja' },
  ];
  readonly filtrosPedidos: { id: FiltroPedidos; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'porEnviar', label: 'Por enviar' },
    { id: 'enCamino', label: 'En camino' },
    { id: 'finalizados', label: 'Finalizados' },
  ];

  // ---- Estado del editor ----
  readonly edEsNuevo = signal(true);
  readonly edActivo = signal(true);
  readonly confirmandoBaja = signal(false);
  readonly leyendoEtiqueta = signal(false);
  readonly avisoEtiqueta = signal<string | null>(null);
  private edId = 0;
  edNombre = '';
  edMarca = '';
  edCategoria = '';
  edPrecio = '';
  edStock = '0';
  edInvima = '';
  edPh = '';
  edDescripcion = '';
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
    this.ruta.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.vista.set(this.vistaDeLaUrl(params.get('vista')));
      // Al volver a una vista se releen los datos: el stock y los pedidos cambian con cada compra.
      if (this.vista() !== 'editor' && this.vista() !== 'ajustes') this.recargar();
    });
  }

  private vistaDeLaUrl(valor: string | null): Vista {
    return VISTAS.includes(valor as Vista) ? (valor as Vista) : 'dashboard';
  }

  recargar(): void {
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
    this.errorVentas.set(null);
    this.api.ventas().subscribe({
      next: (v) => {
        this.resumen.set(v.resumen);
        this.ventas.set(v.ventas);
        this.cargandoVentas.set(false);
      },
      error: (e) => {
        this.errorVentas.set(mensajeDeError(e));
        this.cargandoVentas.set(false);
      },
    });
  }

  private ir(vista: Vista): void {
    this.vista.set(vista);
    this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista } });
  }

  verProductos(filtro: FiltroProductos): void {
    this.filtro.set(filtro);
    this.q.set('');
    this.ir('productos');
  }

  verPedidos(): void {
    this.filtroPedidos.set('todos');
    this.ir('pedidos');
  }

  verPedido(id: number): void {
    this.filtroPedidos.set('todos');
    this.ventaAbierta.set(id);
    this.ir('pedidos');
  }

  stockBajo(p: ProductoVendedor): boolean {
    return p.activo && p.stock <= STOCK_BAJO;
  }

  /** Lo que le falta a un producto publicado para estar completo ante el cliente. */
  faltantes(p: ProductoVendedor): string[] {
    if (!p.activo) return [];
    return [...(p.imagenes?.length ? [] : ['imagen']), ...(p.registroInvima ? [] : ['registro INVIMA'])];
  }

  private grupoDe(v: VentaVendedor): FiltroPedidos {
    const etapa = v.envio?.estado;
    if (etapa === 'ENTREGADO' || etapa === 'NO_ENTREGADO') return 'finalizados';
    if (etapa === 'EN_CAMINO' || v.estado === 'ENVIADO') return 'enCamino';
    return 'porEnviar';
  }

  estadoVenta(v: VentaVendedor): { txt: string; cls: string } {
    switch (v.envio?.estado) {
      case 'ENTREGADO': return { txt: 'Entregado', cls: 'pill-ok' };
      case 'NO_ENTREGADO': return { txt: 'No entregado', cls: 'pill-rojo' };
      case 'EN_CAMINO': return { txt: 'En camino', cls: 'pill-azul' };
      case 'LISTO_PARA_ENVIO': return { txt: 'Listo para envío', cls: 'pill-amber' };
      case 'PREPARANDO': return { txt: 'Preparando', cls: 'pill-amber' };
      default: return v.estado === 'ENVIADO' ? { txt: 'Enviado', cls: 'pill-azul' } : { txt: 'Pagado', cls: 'pill-amber' };
    }
  }

  unidades(v: VentaVendedor): number {
    return v.items.reduce((n, i) => n + i.cantidad, 0);
  }

  fecha(iso: string): string {
    return new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
  }

  alternarVenta(id: number): void {
    this.ventaAbierta.update((actual) => (actual === id ? null : id));
  }

  formatearCop(n: number): string {
    return this.cop.format(n);
  }

  estadoDe(p: ProductoVendedor): { txt: string; cls: string } {
    if (!p.activo) return { txt: 'Dado de baja', cls: 'amber' };
    if (p.stock === 0) return { txt: 'Sin stock', cls: 'rojo' };
    return { txt: 'Publicado', cls: 'ok' };
  }

  irA(id: Vista): void {
    if (id === 'editor') this.abrirNuevo();
    else this.ir(id);
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
    this.edActivo.set(true);
    this.confirmandoBaja.set(false);
    this.avisoEtiqueta.set(null);
    this.edId = 0;
    this.edNombre = '';
    this.edMarca = '';
    this.edCategoria = '';
    this.edPrecio = '';
    this.edStock = '0';
    this.edInvima = '';
    this.edPh = '';
    this.edDescripcion = '';
    this.edImagenes.set(['']);
    this.edFicha.set([]);
    this.limpiarAddInci();
    this.edError.set(null);
    this.ir('editor');
  }

  abrirEditor(p: ProductoVendedor): void {
    this.edEsNuevo.set(false);
    this.edActivo.set(p.activo);
    this.confirmandoBaja.set(false);
    this.avisoEtiqueta.set(null);
    this.edId = p.id;
    this.edNombre = p.nombre;
    this.edMarca = p.marca ?? '';
    this.edCategoria = p.categoria ?? '';
    this.edPrecio = String(p.precio ?? '');
    this.edStock = String(p.stock ?? '0');
    this.edInvima = p.registroInvima ?? '';
    this.edPh = p.ph != null ? String(p.ph) : '';
    this.edDescripcion = p.descripcion ?? '';
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
    this.ir('editor');
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
    if (this.edFicha().some((x) => x.inci === elegido.inci)) {
      this.edError.set(`«${elegido.inci}» ya está en la ficha.`);
      return;
    }
    this.edError.set(null);
    this.edFicha.update((f) => [...f, { inci: elegido.inci, descripcion: elegido.descripcion ?? '', concentracion: this.edConc }]);
    this.limpiarAddInci();
  }
  quitarInci(nombreInci: string): void {
    this.edFicha.update((f) => f.filter((x) => x.inci !== nombreInci));
  }

  /** Lee la foto de la etiqueta y agrega a la ficha, en el mismo orden, lo que está en el catálogo (HU5). */
  leerEtiqueta(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const foto = campo.files?.[0];
    campo.value = ''; // permite volver a elegir la misma foto
    if (!foto) return;
    this.edError.set(null);
    this.avisoEtiqueta.set(null);
    if (!foto.type.startsWith('image/')) {
      this.edError.set('Elige una imagen de la etiqueta (JPG o PNG).');
      return;
    }
    if (foto.size > MAX_FOTO_BYTES) {
      this.edError.set('La foto pesa más de 8 MB. Prueba con una más liviana.');
      return;
    }
    this.leyendoEtiqueta.set(true);
    this.api.leerEtiqueta(foto).subscribe({
      next: ({ ingredientes, enCatalogo }) => {
        const ya = new Set(this.edFicha().map((x) => x.inci));
        const nuevos = enCatalogo.filter((i) => !ya.has(i.inci));
        this.edFicha.update((f) => [...f, ...nuevos.map((i) => ({ inci: i.inci, descripcion: i.descripcion ?? '', concentracion: '' }))]);
        const reconocidos = new Set(enCatalogo.map((i) => i.inci.toLowerCase()));
        const fuera = ingredientes.filter((n) => !reconocidos.has(n.toLowerCase()));
        this.avisoEtiqueta.set(`Agregamos ${nuevos.length} de ${ingredientes.length} ingredientes. Revisa la ficha contra la etiqueta.`
          + (fuera.length ? ` No están en el catálogo: ${fuera.join(', ')}.` : ''));
        this.leyendoEtiqueta.set(false);
      },
      error: (e) => {
        this.edError.set(e?.status >= 500 ? 'No pudimos leer la etiqueta ahora. Agrega los ingredientes a mano o inténtalo más tarde.' : mensajeDeError(e));
        this.leyendoEtiqueta.set(false);
      },
    });
  }

  private errorDelFormulario(): string | null {
    if (!this.edNombre.trim()) return 'Escribe el nombre del producto.';
    if (!(Number(this.edPrecio) > 0)) return 'Escribe un precio mayor que cero.';
    if (String(this.edStock ?? '').trim() === '' || !Number.isInteger(Number(this.edStock)) || Number(this.edStock) < 0) {
      return 'El stock debe ser un número entero, cero o mayor.';
    }
    const ph = String(this.edPh ?? '').trim();
    if (ph !== '' && (Number(ph) < 0 || Number(ph) > 14)) return 'El pH debe estar entre 0 y 14.';
    if (this.edFicha().length === 0) return 'La ficha INCI debe tener al menos un ingrediente para poder guardar.';
    return null;
  }

  private datos(): DatosProducto {
    return {
      nombre: this.edNombre.trim(),
      marca: this.edMarca.trim() || undefined,
      categoria: this.edCategoria.trim() || undefined,
      precio: Number(this.edPrecio),
      stock: Number(this.edStock),
      // Antes se enviaba siempre null y editar un producto borraba su pH.
      ph: String(this.edPh ?? '').trim() === '' ? null : Number(this.edPh),
      registroInvima: this.edInvima.trim() || undefined,
      descripcion: this.edDescripcion.trim() || undefined,
      imagenes: this.edImagenes().map((u) => u.trim()).filter(Boolean),
      ingredientes: this.edFicha().map((f) => ({
        inci: f.inci,
        concentracion: f.concentracion ? Number(f.concentracion) : null,
      })),
    };
  }

  guardar(): void {
    const problema = this.errorDelFormulario();
    if (problema) {
      this.edError.set(problema);
      return;
    }
    this.edError.set(null);
    this.edOcupado.set(true);
    const peticion = this.edEsNuevo() ? this.api.crear(this.datos()) : this.api.actualizar(this.edId, this.datos());
    peticion.subscribe({
      next: (p) => {
        this.aplicar(p);
        this.edOcupado.set(false);
        this.ir('productos');
      },
      error: (e) => {
        this.edError.set(mensajeDeError(e));
        this.edOcupado.set(false);
      },
    });
  }

  volverAPublicar(): void {
    this.edError.set(null);
    this.edOcupado.set(true);
    this.api.publicar(this.edId).subscribe({
      next: (p) => {
        this.aplicar(p);
        this.edActivo.set(true);
        this.edOcupado.set(false);
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
        this.confirmandoBaja.set(false);
        this.ir('productos');
      },
      error: (e) => {
        this.edError.set(mensajeDeError(e));
        this.edOcupado.set(false);
      },
    });
  }
}
