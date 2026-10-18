import { Link } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthContext'

/** Panel del CLIENTE: compra, perfil dermatológico y compatibilidad. */
export default function ClientePanel() {
  const { sesion } = useAuth()
  return (
    <section>
      <h2>Hola, {sesion?.nombre} 👋</h2>
      <p>Este es tu espacio como <strong>cliente</strong> de GlossUP.</p>
      <div className="panel-grid">
        <Link className="card tile" to="/catalogo">🛍️ Explorar catálogo</Link>
        <Link className="card tile" to="/compatibilidad">🧪 Analizar compatibilidad</Link>
        <Link className="card tile" to="/perfil">🙂 Mi perfil dermatológico</Link>
        <Link className="card tile" to="/carrito">🛒 Mi carrito</Link>
      </div>
    </section>
  )
}
