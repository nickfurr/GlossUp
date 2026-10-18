import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import HomePage from './HomePage'
import LoginPage from '../features/usuarios/pages/LoginPage'
import PerfilPage from '../features/perfil/pages/PerfilPage'
import CatalogoPage from '../features/catalogo/pages/CatalogoPage'
import CompatibilidadPage from '../features/compatibilidad/pages/CompatibilidadPage'
import CarritoPage from '../features/recomendacion/pages/CarritoPage'
import ClientePanel from '../features/panel/ClientePanel'
import VendedorPanel from '../features/panel/VendedorPanel'
import AdminPanel from '../features/panel/AdminPanel'
import { AuthProvider, useAuth } from '../shared/auth/AuthContext'
import { RutaProtegida } from '../shared/auth/RutaProtegida'
import { rutaDePanel } from '../shared/auth/auth'

/** Shell de la aplicación: barra de navegación adaptada al rol + enrutado con guards. */
function AppShell() {
  const { sesion, salir } = useAuth()
  const navigate = useNavigate()

  function cerrarSesion() {
    salir()
    navigate('/login', { replace: true })
  }

  // El vendedor usa su propio layout con barra lateral; el login/registro no llevan barra.
  // Solo se muestra la barra superior a clientes y admins ya autenticados.
  const mostrarTopbar = !!sesion && sesion.panel !== 'VENDEDOR'

  return (
    <>
      {mostrarTopbar && (
      <header className="topbar">
        <Link to={sesion ? rutaDePanel(sesion.panel) : '/'} className="brand">GlossUP</Link>
        <nav>
          {sesion?.panel === 'CLIENTE' && (
            <>
              <Link to="/catalogo">Catálogo</Link>
              <Link to="/compatibilidad">Compatibilidad</Link>
              <Link to="/perfil">Mi perfil</Link>
              <Link to="/carrito">Carrito</Link>
            </>
          )}
          {sesion ? (
            <>
              <Link to={rutaDePanel(sesion.panel)} className="user">👤 {sesion.nombre} ({sesion.panel})</Link>
              <button type="button" className="link" onClick={cerrarSesion}>Salir</button>
            </>
          ) : (
            <Link to="/login">Entrar</Link>
          )}
        </nav>
      </header>
      )}

      <main className={sesion?.panel === 'VENDEDOR' ? 'full' : 'container'}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<Inicio />} />

          {/* Panel por rol: cada guard "deja pasar" solo a su tipo de usuario */}
          <Route path="/cliente" element={<RutaProtegida roles={['CLIENTE']}><ClientePanel /></RutaProtegida>} />
          <Route path="/vendedor" element={<RutaProtegida roles={['VENDEDOR']}><VendedorPanel /></RutaProtegida>} />
          <Route path="/admin" element={<RutaProtegida roles={['ADMIN']}><AdminPanel /></RutaProtegida>} />

          {/* Zona del cliente (perfil, catálogo, compatibilidad, carrito) */}
          <Route path="/perfil" element={<RutaProtegida roles={['CLIENTE']}><PerfilPage /></RutaProtegida>} />
          <Route path="/catalogo" element={<RutaProtegida roles={['CLIENTE']}><CatalogoPage /></RutaProtegida>} />
          <Route path="/compatibilidad" element={<RutaProtegida roles={['CLIENTE']}><CompatibilidadPage /></RutaProtegida>} />
          <Route path="/carrito" element={<RutaProtegida roles={['CLIENTE']}><CarritoPage /></RutaProtegida>} />

          <Route path="/home" element={<HomePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}

/** Raíz: si hay sesión lleva a su panel; si no, al login. */
function Inicio() {
  const { sesion } = useAuth()
  return <Navigate to={sesion ? rutaDePanel(sesion.panel) : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppShell />
      </BrowserRouter>
    </AuthProvider>
  )
}
