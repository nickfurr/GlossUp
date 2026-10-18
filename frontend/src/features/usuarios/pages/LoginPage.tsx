import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../../shared/auth/AuthContext'
import { rutaDePanel } from '../../../shared/auth/auth'
import type { RolUsuario } from '../../../shared/types'

/**
 * Inicio de sesión y registro (mockups slides 3-4): tarjeta a dos columnas — panel de marca vino
 * a la izquierda y formulario a la derecha; responsive (en móvil el panel de marca se oculta).
 * Tras autenticarse redirige al panel del rol.
 */
export default function LoginPage() {
  const { entrar, registrar } = useAuth()
  const navigate = useNavigate()

  const [modo, setModo] = useState<'login' | 'registro'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [nombre, setNombre] = useState('')
  const [rol, setRol] = useState<RolUsuario>('CLIENTE')
  const [acepta, setAcepta] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const esRegistro = modo === 'registro'

  function cambiarModo() {
    setModo(esRegistro ? 'login' : 'registro')
    setError(null)
    setInfo(null)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (esRegistro && password !== password2) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setCargando(true)
    try {
      if (esRegistro) await registrar({ email, nombre, password, rol })
      const sesion = await entrar(email, password)
      navigate(rutaDePanel(sesion.panel), { replace: true })
    } catch (err: unknown) {
      setError(mensajeDeError(err, modo))
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="auth-split">
      {/* Panel de marca (izquierda) */}
      <aside className="auth-marca">
        <div className="auth-marca-logo">GlossUP</div>
        <p className="auth-marca-tag">Tu piel, en buenas manos</p>
        {esRegistro ? (
          <p className="auth-marca-sub">Únete a GlossUP y compra con seguridad dermatológica.</p>
        ) : (
          <ul className="auth-ventajas">
            <li>
              <span className="tick">✓</span> Compatibilidad por ingredientes
            </li>
            <li>
              <span className="tick">✓</span> Recomendaciones con IA
            </li>
            <li>
              <span className="tick">✓</span> Perfil dermatológico
            </li>
          </ul>
        )}
      </aside>

      {/* Formulario (derecha) */}
      <div className="auth-form-col">
        <h2>{esRegistro ? 'Crea tu cuenta' : 'Iniciar sesión'}</h2>

        <form onSubmit={onSubmit} className="auth-form">
          {esRegistro && (
            <div className="campo-full">
              <span className="etiqueta">Tipo de cuenta</span>
              <div className="toggle-rol">
                <button
                  type="button"
                  className={rol === 'CLIENTE' ? 'activo' : ''}
                  onClick={() => setRol('CLIENTE')}
                >
                  Cliente
                </button>
                <button
                  type="button"
                  className={rol === 'VENDEDOR' ? 'activo' : ''}
                  onClick={() => setRol('VENDEDOR')}
                >
                  Vendedor
                </button>
              </div>
            </div>
          )}

          {esRegistro && (
            <label>
              Nombre completo
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre y apellidos" required />
            </label>
          )}

          <label>
            Correo electrónico
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="correo@ejemplo.com" required />
          </label>

          <div className={esRegistro ? 'campos-2' : 'campo-full'}>
            <label>
              Contraseña
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={esRegistro ? 'Mínimo 8 caracteres' : '••••••••'}
                minLength={esRegistro ? 8 : undefined}
                required
              />
            </label>
            {esRegistro && (
              <label>
                Confirmar
                <input
                  type="password"
                  value={password2}
                  onChange={(e) => setPassword2(e.target.value)}
                  placeholder="Repite la contraseña"
                  required
                />
              </label>
            )}
          </div>

          {!esRegistro && (
            <button type="button" className="link olvido" onClick={() => setInfo('Recuperación de contraseña: próximamente.')}>
              ¿Olvidaste tu contraseña?
            </button>
          )}

          {esRegistro && (
            <label className="terminos">
              <input type="checkbox" checked={acepta} onChange={(e) => setAcepta(e.target.checked)} required />
              Acepto los Términos y la Política de datos (Ley 1581/2012).
            </label>
          )}

          {error && <p className="error">{error}</p>}
          {info && <p className="nota">{info}</p>}

          <button type="submit" className="btn-primario ancho" disabled={cargando}>
            {cargando ? 'Procesando…' : esRegistro ? 'Crear cuenta' : 'Ingresar'}
          </button>

          {!esRegistro && (
            <>
              <div className="separador">o continúa con</div>
              <button
                type="button"
                className="btn-sec ancho"
                onClick={() => setInfo('Inicio con Google: próximamente.')}
              >
                Google
              </button>
            </>
          )}
        </form>

        <p className="auth-switch">
          {esRegistro ? '¿Ya tienes cuenta?' : '¿No tienes cuenta?'}{' '}
          <button type="button" className="link" onClick={cambiarModo}>
            {esRegistro ? 'Inicia sesión' : 'Regístrate'}
          </button>
        </p>
      </div>
    </section>
  )
}

function mensajeDeError(err: unknown, modo: 'login' | 'registro'): string {
  const e = err as { response?: { status?: number; data?: { message?: string } } }
  const status = e?.response?.status
  if (status === 401) return 'Correo o contraseña incorrectos.'
  if (status === 422) return e?.response?.data?.message ?? 'Ese correo ya está registrado.'
  if (status === 400) return 'Revisa los datos: la contraseña debe tener al menos 8 caracteres.'
  // El backend aún no expone registro: avisamos en vez de mostrar un error técnico.
  if (modo === 'registro' && (status === 404 || status === 405)) {
    return 'El registro aún no está habilitado en el servidor. Pídele a un administrador que cree tu cuenta.'
  }
  return modo === 'login' ? 'No se pudo iniciar sesión.' : 'No se pudo crear la cuenta.'
}
