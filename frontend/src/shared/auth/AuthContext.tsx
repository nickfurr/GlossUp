import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import {
  login as loginApi,
  logout as logoutApi,
  registrar as registrarApi,
  sesionActual,
  type SesionUsuario,
} from './auth'
import type { RolUsuario } from '../types'

interface AuthContextValor {
  sesion: SesionUsuario | null
  entrar: (email: string, password: string) => Promise<SesionUsuario>
  registrar: (input: { email: string; nombre: string; password: string; rol?: RolUsuario }) => Promise<void>
  salir: () => void
}

const AuthContext = createContext<AuthContextValor | null>(null)

/** Provee la sesión actual a toda la app y la mantiene sincronizada con el almacenamiento. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<SesionUsuario | null>(() => sesionActual())

  const entrar = useCallback(async (email: string, password: string) => {
    const s = await loginApi(email, password)
    setSesion(s)
    return s
  }, [])

  const salir = useCallback(() => {
    logoutApi()
    setSesion(null)
  }, [])

  const valor = useMemo<AuthContextValor>(
    () => ({ sesion, entrar, registrar: registrarApi, salir }),
    [sesion, entrar, salir],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValor {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
