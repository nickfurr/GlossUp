import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { rutaDePanel, type Panel } from './auth'

/**
 * Guarda de ruta: protege un panel/zona de la app.
 *
 * - Si no hay sesión → redirige a /login.
 * - Si hay sesión pero el rol no está permitido → lo "deja pasar" solo a su propio panel
 *   (redirige al panel que le corresponde), de modo que cada uno de los tres tipos de usuario
 *   solo accede a su área.
 */
export function RutaProtegida({ roles, children }: { roles?: Panel[]; children: ReactNode }) {
  const { sesion } = useAuth()

  if (!sesion) {
    return <Navigate to="/login" replace />
  }
  if (roles && !roles.includes(sesion.panel)) {
    return <Navigate to={rutaDePanel(sesion.panel)} replace />
  }
  return <>{children}</>
}
