import type { SessionUser } from './types'

const TOKEN_KEY = 'prestamos.accessToken'
const USER_KEY = 'prestamos.sessionUser'

export type StoredSession = { tokenAcceso: string; usuario: SessionUser }

export function readSession(): StoredSession | null {
  try {
    const tokenAcceso = sessionStorage.getItem(TOKEN_KEY)
    const rawUser = sessionStorage.getItem(USER_KEY)
    if (!tokenAcceso || !rawUser) return null
    return { tokenAcceso, usuario: JSON.parse(rawUser) as SessionUser }
  } catch {
    return null
  }
}

export function saveSession(session: StoredSession) {
  sessionStorage.setItem(TOKEN_KEY, session.tokenAcceso)
  sessionStorage.setItem(USER_KEY, JSON.stringify(session.usuario))
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}
