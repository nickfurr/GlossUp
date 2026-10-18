import { useAuth } from '../../shared/auth/AuthContext'

/** Panel del ADMINISTRADOR: administración de la plataforma. */
export default function AdminPanel() {
  const { sesion } = useAuth()
  return (
    <section>
      <h2>Panel de administración — {sesion?.nombre}</h2>
      <p>Administración de la plataforma GlossUP.</p>
      <div className="panel-grid">
        <div className="card tile">👥 Usuarios y roles</div>
        <div className="card tile">🧬 Reglas de incompatibilidad <small>(ontología)</small></div>
        <div className="card tile">🏷️ Marcas y catálogos</div>
        <div className="card tile">📊 Métricas y reportes</div>
      </div>
      <p className="nota">Funcionalidad en construcción.</p>
    </section>
  )
}
