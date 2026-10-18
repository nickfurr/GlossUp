import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthContext'
import {
  actualizarProducto,
  crearProducto,
  eliminarProducto,
  listarMisProductos,
  mensajeDeError,
  type DatosProducto,
  type ProductoVendedor,
} from '../../features/catalogo/api/productos'

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

type Vista = 'dashboard' | 'productos' | 'editor' | 'pedidos' | 'ventas' | 'ajustes'

/** Estado comercial de un producto para la etiqueta de la tabla. */
function estadoDe(p: ProductoVendedor): { txt: string; cls: string } {
  if (!p.activo) return { txt: 'Inactivo', cls: 'amber' }
  if (p.stock === 0) return { txt: 'Sin stock', cls: 'rojo' }
  return { txt: 'Indexado', cls: 'ok' }
}

/**
 * Panel del VENDEDOR (mockups 15-17): barra lateral + Dashboard, Mis productos (tabla) y el editor
 * a dos columnas con la ficha INCI. Habla con la API de catálogo: crear/editar envían el producto
 * completo (datos + imágenes + ficha INCI, mínimo un ingrediente); eliminar es un borrado lógico.
 */
export default function VendedorPanel() {
  const { sesion, salir } = useAuth()
  const navigate = useNavigate()
  const [productos, setProductos] = useState<ProductoVendedor[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [vista, setVista] = useState<Vista>('dashboard')
  const [editando, setEditando] = useState<ProductoVendedor | 'nuevo' | null>(null)

  useEffect(() => {
    listarMisProductos()
      .then(setProductos)
      .catch((e) => setError(mensajeDeError(e)))
      .finally(() => setCargando(false))
  }, [])

  function aplicar(p: ProductoVendedor) {
    setProductos((prev) => {
      const i = prev.findIndex((x) => x.id === p.id)
      if (i === -1) return [p, ...prev]
      const copia = [...prev]
      copia[i] = p
      return copia
    })
  }

  function abrirNuevo() {
    setEditando('nuevo')
    setVista('editor')
  }
  function abrirEditor(p: ProductoVendedor) {
    setEditando(p)
    setVista('editor')
  }

  function cerrarSesion() {
    salir()
    navigate('/login', { replace: true })
  }

  const nav: { id: Vista; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'productos', label: 'Mis productos' },
    { id: 'editor', label: 'Nuevo producto' },
    { id: 'pedidos', label: 'Pedidos' },
    { id: 'ventas', label: 'Ventas' },
    { id: 'ajustes', label: 'Ajustes' },
  ]

  return (
    <div className="vend-layout">
      <aside className="vend-side">
        <div className="vend-brand">GlossUP</div>
        <div className="vend-sub">Panel Vendedor</div>
        <nav className="vend-nav">
          {nav.map((n) => (
            <button
              key={n.id}
              type="button"
              className={vista === n.id ? 'activo' : ''}
              onClick={() => (n.id === 'editor' ? abrirNuevo() : setVista(n.id))}
            >
              <span className="dot" />
              {n.label}
            </button>
          ))}
        </nav>
        <div className="vend-side-pie">
          <span>{sesion?.nombre}</span>
          <button type="button" onClick={cerrarSesion}>
            Salir
          </button>
        </div>
      </aside>

      <main className="vend-main">
        {error && <p className="error">{error}</p>}
        {vista === 'dashboard' && (
          <Dashboard marca={sesion?.nombre ?? ''} productos={productos} onNuevo={abrirNuevo} />
        )}
        {vista === 'productos' && (
          <MisProductos productos={productos} cargando={cargando} onEditar={abrirEditor} onNuevo={abrirNuevo} />
        )}
        {vista === 'editor' && (
          <ProductoEditor
            inicial={editando === 'nuevo' ? null : (editando as ProductoVendedor)}
            onGuardado={(p) => {
              aplicar(p)
              setVista('productos')
            }}
            onVolver={() => setVista('productos')}
          />
        )}
        {(vista === 'pedidos' || vista === 'ventas' || vista === 'ajustes') && (
          <Placeholder titulo={nav.find((n) => n.id === vista)!.label} />
        )}
      </main>
    </div>
  )
}

/* ------------------------------------------------------------------ Dashboard */
function Dashboard({
  marca,
  productos,
  onNuevo,
}: {
  marca: string
  productos: ProductoVendedor[]
  onNuevo: () => void
}) {
  const activos = productos.filter((p) => p.activo).length
  const alertas = productos.filter((p) => p.ingredientes.length === 0).length

  return (
    <>
      <div className="vend-topline">
        <h2>Buenos días, {marca}</h2>
        <button type="button" className="btn-primario" onClick={onNuevo}>
          ＋ Nuevo producto
        </button>
      </div>

      <div className="tiles">
        <StatTile label="Ventas del mes" valor={COP.format(0)} cls="val-vino" />
        <StatTile label="Pedidos" valor="0" cls="val-rosa" />
        <StatTile label="Productos activos" valor={String(activos)} cls="val-ok" />
        <StatTile label="Alertas de INCI" valor={String(alertas)} cls="val-amber" />
      </div>

      <div className="dash-row">
        <section className="panel">
          <h3>Ventas por semana</h3>
          <p className="nota">Aún no hay ventas registradas.</p>
        </section>
        <section className="panel">
          <h3>Pedidos recientes</h3>
          <p className="nota">No hay pedidos aún.</p>
        </section>
      </div>
    </>
  )
}

function StatTile({ label, valor, cls }: { label: string; valor: string; cls: string }) {
  return (
    <div className="tile-stat">
      <span className="label">{label}</span>
      <span className={`val ${cls}`}>{valor}</span>
    </div>
  )
}

/* ------------------------------------------------------------- Mis productos */
function MisProductos({
  productos,
  cargando,
  onEditar,
  onNuevo,
}: {
  productos: ProductoVendedor[]
  cargando: boolean
  onEditar: (p: ProductoVendedor) => void
  onNuevo: () => void
}) {
  const [q, setQ] = useState('')
  const [filtro, setFiltro] = useState<'todos' | 'activos' | 'sinstock'>('todos')

  const lista = useMemo(() => {
    const texto = q.trim().toLowerCase()
    return productos.filter((p) => {
      if (filtro === 'activos' && !p.activo) return false
      if (filtro === 'sinstock' && p.stock !== 0) return false
      if (!texto) return true
      const enNombre = p.nombre.toLowerCase().includes(texto)
      const enInci = p.ingredientes.some((i) => i.inci.toLowerCase().includes(texto))
      return enNombre || enInci
    })
  }, [productos, q, filtro])

  return (
    <>
      <div className="vend-topline">
        <h2>Mis productos</h2>
        <button type="button" className="btn-primario" onClick={onNuevo}>
          ＋ Nuevo producto
        </button>
      </div>

      <div className="filtros-fila">
        <input
          className="buscador"
          placeholder="Buscar por nombre o ingrediente (INCI)"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="filtros">
          {(['todos', 'activos', 'sinstock'] as const).map((f) => (
            <button
              key={f}
              type="button"
              className={`filtro ${filtro === f ? 'activo' : ''}`}
              onClick={() => setFiltro(f)}
            >
              {f === 'todos' ? 'Todos' : f === 'activos' ? 'Activos' : 'Sin stock'}
            </button>
          ))}
        </div>
      </div>

      <div className="panel">
        {cargando ? (
          <p className="nota">Cargando…</p>
        ) : lista.length === 0 ? (
          <p className="nota">No hay productos que coincidan.</p>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th>PRODUCTO</th>
                <th>FICHA INCI</th>
                <th>PRECIO</th>
                <th>STOCK</th>
                <th>ESTADO</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((p) => {
                const est = estadoDe(p)
                return (
                  <tr key={p.id} onClick={() => onEditar(p)} className="fila-click">
                    <td className="celda-prod">
                      <span className="thumb">
                        {p.imagenes?.[0] ? <img src={p.imagenes[0]} alt={p.nombre} /> : null}
                      </span>
                      {p.nombre}
                    </td>
                    <td>{p.ingredientes.length} ingredientes</td>
                    <td className="precio">{COP.format(p.precio)}</td>
                    <td className={p.stock === 0 ? 'rojo-txt' : ''}>{p.stock}</td>
                    <td>
                      <span className={`badge-estado ${est.cls}`}>{est.txt}</span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
      <p className="nota">Haz clic en un producto para editarlo, gestionar su ficha INCI o darlo de baja.</p>
    </>
  )
}

/* ------------------------------------------------- Editor (crear / editar) */
interface InciLocal {
  inci: string
  descripcion: string
  concentracion: string
}

function ProductoEditor({
  inicial,
  onGuardado,
  onVolver,
}: {
  inicial: ProductoVendedor | null
  onGuardado: (p: ProductoVendedor) => void
  onVolver: () => void
}) {
  const esNuevo = inicial === null
  const [nombre, setNombre] = useState(inicial?.nombre ?? '')
  const [categoria, setCategoria] = useState(inicial?.categoria ?? '')
  const [precio, setPrecio] = useState(String(inicial?.precio ?? ''))
  const [stock, setStock] = useState(String(inicial?.stock ?? '0'))
  const [invima, setInvima] = useState(inicial?.registroInvima ?? '')
  const [imagenes, setImagenes] = useState<string[]>(
    inicial?.imagenes?.length ? inicial.imagenes : [''],
  )
  const [ficha, setFicha] = useState<InciLocal[]>(
    (inicial?.ingredientes ?? []).map((i) => ({
      inci: i.inci,
      descripcion: i.descripcion ?? '',
      concentracion: i.concentracion != null ? String(i.concentracion) : '',
    })),
  )
  const [inci, setInci] = useState('')
  const [funcion, setFuncion] = useState('')
  const [conc, setConc] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  function agregarInci(e: React.FormEvent) {
    e.preventDefault()
    const nombreInci = inci.trim()
    if (!nombreInci) return
    if (ficha.some((x) => x.inci.toLowerCase() === nombreInci.toLowerCase())) return
    setFicha([...ficha, { inci: nombreInci, descripcion: funcion.trim(), concentracion: conc }])
    setInci('')
    setFuncion('')
    setConc('')
  }

  function quitarInci(nombreInci: string) {
    setFicha(ficha.filter((x) => x.inci !== nombreInci))
  }

  function cambiarImagen(idx: number, valor: string) {
    setImagenes(imagenes.map((u, i) => (i === idx ? valor : u)))
  }

  function datos(): DatosProducto {
    return {
      nombre: nombre.trim(),
      categoria: categoria.trim() || undefined,
      precio: Number(precio),
      stock: Number(stock),
      ph: null,
      registroInvima: invima.trim() || undefined,
      imagenes: imagenes.map((u) => u.trim()).filter(Boolean),
      ingredientes: ficha.map((f) => ({
        inci: f.inci,
        descripcion: f.descripcion.trim() || null,
        concentracion: f.concentracion ? Number(f.concentracion) : null,
      })),
    }
  }

  async function guardar() {
    if (ficha.length === 0) {
      setError('La ficha INCI debe tener al menos un ingrediente para poder guardar.')
      return
    }
    setError(null)
    setOcupado(true)
    try {
      const p = esNuevo ? await crearProducto(datos()) : await actualizarProducto(inicial!.id, datos())
      onGuardado(p)
    } catch (e) {
      setError(mensajeDeError(e))
      setOcupado(false)
    }
  }

  async function eliminar() {
    if (esNuevo || !inicial) return
    setError(null)
    setOcupado(true)
    try {
      await eliminarProducto(inicial.id)
      onGuardado({ ...inicial, activo: false })
    } catch (e) {
      setError(mensajeDeError(e))
      setOcupado(false)
    }
  }

  return (
    <>
      <div className="vend-topline">
        <h2>{esNuevo ? 'Nuevo producto' : `Editar: ${inicial?.nombre}`}</h2>
        <button type="button" className="btn-sec" onClick={onVolver}>
          ← Volver
        </button>
      </div>

      <div className="editor">
        {/* Columna izquierda: datos comerciales */}
        <section className="panel">
          <h3>Datos comerciales</h3>
          <label className="campo">
            Nombre del producto
            <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Ej: Serum Vitamina C 10%" maxLength={200} />
          </label>
          <div className="campos-2">
            <label className="campo">
              Categoría
              <input value={categoria} onChange={(e) => setCategoria(e.target.value)} maxLength={120} />
            </label>
            <label className="campo">
              Precio (COP)
              <input type="number" min={0} value={precio} onChange={(e) => setPrecio(e.target.value)} />
            </label>
          </div>
          <div className="campos-2">
            <label className="campo">
              Stock
              <input type="number" min={0} value={stock} onChange={(e) => setStock(e.target.value)} />
            </label>
            <label className="campo">
              Registro INVIMA
              <input value={invima} onChange={(e) => setInvima(e.target.value)} maxLength={100} placeholder="NSOC01234-56CO" />
            </label>
          </div>

          <span className="etiqueta">Imágenes (URL)</span>
          {imagenes.map((u, i) => (
            <div key={i} className="img-fila">
              <input type="url" value={u} onChange={(e) => cambiarImagen(i, e.target.value)} placeholder="https://…" maxLength={500} />
              {imagenes.length > 1 && (
                <button type="button" className="quitar" aria-label="Quitar imagen" onClick={() => setImagenes(imagenes.filter((_, j) => j !== i))}>
                  ×
                </button>
              )}
            </div>
          ))}
          <button type="button" className="btn-mini" onClick={() => setImagenes([...imagenes, ''])}>
            ＋ Agregar imagen
          </button>
          <div className="dropzone">
            {imagenes.find((u) => u.trim()) ? (
              <img src={imagenes.find((u) => u.trim())} alt="Vista previa" />
            ) : (
              <span>Pega el enlace de una imagen arriba</span>
            )}
          </div>
        </section>

        {/* Columna derecha: ficha de ingredientes (INCI) */}
        <section className="panel">
          <h3>Ficha de ingredientes (INCI)</h3>
          <table className="inci-tabla">
            <thead>
              <tr>
                <th>INGREDIENTE (INCI)</th>
                <th>FUNCIÓN</th>
                <th>CONC.</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {ficha.length === 0 ? (
                <tr>
                  <td colSpan={4} className="nota">
                    Sin ingredientes. Añade al menos uno (obligatorio para guardar).
                  </td>
                </tr>
              ) : (
                ficha.map((i) => (
                  <tr key={i.inci}>
                    <td>{i.inci}</td>
                    <td className="suave">{i.descripcion || '—'}</td>
                    <td>{i.concentracion ? `${i.concentracion}%` : '—'}</td>
                    <td>
                      <button type="button" className="quitar" aria-label={`Quitar ${i.inci}`} onClick={() => quitarInci(i.inci)}>
                        ×
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          <form className="inci-form" onSubmit={agregarInci}>
            <input placeholder="Ingrediente (INCI)" value={inci} onChange={(e) => setInci(e.target.value)} />
            <input placeholder="Función" value={funcion} onChange={(e) => setFuncion(e.target.value)} />
            <input type="number" min={0} max={100} step="0.1" placeholder="%" value={conc} onChange={(e) => setConc(e.target.value)} />
            <button type="submit" className="btn-mini">
              ＋ Agregar activo
            </button>
          </form>

          <p className="nota-amber">⚠️ El sistema mapeará cada ingrediente contra la ontología médica al guardar.</p>
        </section>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="editor-acciones">
        {!esNuevo && (
          <button type="button" className="btn-sec" disabled={ocupado} onClick={eliminar}>
            Dar de baja
          </button>
        )}
        <button type="button" className="btn-primario" disabled={ocupado} onClick={guardar}>
          {ocupado ? 'Guardando…' : esNuevo ? 'Crear producto' : 'Guardar cambios'}
        </button>
      </div>
    </>
  )
}

function Placeholder({ titulo }: { titulo: string }): ReactNode {
  return (
    <>
      <div className="vend-topline">
        <h2>{titulo}</h2>
      </div>
      <div className="panel">
        <p className="nota">Esta sección estará disponible al implementar su módulo (pedidos/ventas).</p>
      </div>
    </>
  )
}
