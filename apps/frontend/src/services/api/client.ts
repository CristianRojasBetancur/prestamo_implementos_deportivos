export type FieldError = { campo: string; mensaje: string }

export type ApiProblem = {
  type?: string
  title?: string
  status?: number
  detail?: string
  codigo?: string
  correlacionId?: string
  errores?: FieldError[]
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code = 'ERROR_RED',
    readonly fieldErrors: FieldError[] = [],
    readonly correlationId?: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

function baseUrl() {
  return import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '')
}

function correlationId() {
  const generated = globalThis.crypto?.randomUUID?.() ?? `frontend-${Date.now()}`
  return generated.slice(0, 64)
}

function token() {
  try {
    return sessionStorage.getItem('prestamos.accessToken')
  } catch {
    return null
  }
}

function safeProblem(problem: ApiProblem, status?: number) {
  const code = problem.codigo ?? (status === 401 ? 'NO_AUTENTICADO' : status === 403 ? 'ACCESO_DENEGADO' : 'ERROR_RED')
  const messages: Record<string, string> = {
    NO_AUTENTICADO: 'Tu sesión no es válida. Inicia sesión nuevamente.',
    SESION_INVALIDA: 'Tu sesión expiró o fue revocada. Inicia sesión nuevamente.',
    CREDENCIALES_INVALIDAS: 'El usuario o la contraseña no son válidos.',
    ACCESO_DENEGADO: 'No tienes permisos para realizar esta operación.',
    RECURSO_NO_ENCONTRADO: 'El recurso solicitado no existe o ya no está disponible.',
    SOLICITUD_INVALIDA: 'La solicitud no tiene un formato válido.',
    VALIDACION_FALLIDA: 'Revisa los datos ingresados e inténtalo de nuevo.',
    CONFLICTO_UNICIDAD: 'Ya existe un registro con esos datos.',
    CONFLICTO_CONCURRENCIA: 'La información cambió mientras realizabas la operación. Actualiza e inténtalo de nuevo.',
    IMPLEMENTO_NO_DISPONIBLE: 'Uno de los implementos ya no está disponible.',
    IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO: 'El implemento seleccionado no pertenece a ese préstamo.',
    UNIDAD_YA_RESUELTA: 'Una de las unidades ya fue devuelta o resuelta.',
    PRESTAMO_FINALIZADO: 'El préstamo ya está finalizado y no admite esta operación.',
    INCIDENCIA_REQUERIDA: 'Debes describir la incidencia de cada implemento con novedad.',
    TIPO_INCIDENCIA_NO_PERMITIDO: 'El tipo de incidencia no aplica para esta operación.',
    JUSTIFICACION_REQUERIDA: 'Debes ingresar una justificación para el retraso.',
    SERVICIO_NO_DISPONIBLE: 'El servicio no está disponible en este momento.',
    ERROR_INTERNO: 'No fue posible completar la operación.',
  }
  const statusMessage = status === 404
    ? 'El recurso solicitado no existe o ya no está disponible.'
    : status === 409
      ? 'La operación entra en conflicto con el estado actual del recurso.'
      : status === 422
        ? 'La operación no cumple una regla del proceso.'
        : 'No fue posible completar la solicitud.'
  return new ApiError(messages[code] ?? statusMessage, status, code, problem.errores ?? [], problem.correlacionId)
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const url = baseUrl()
  if (!url) throw new ApiError('La API no está configurada para este entorno.', undefined, 'API_NO_CONFIGURADA')

  const id = correlationId()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  headers.set('X-Correlation-Id', id)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const accessToken = token()
  if (accessToken && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${accessToken}`)

  try {
    const response = await fetch(`${url}${path}`, { ...init, headers })
    const contentType = response.headers.get('content-type') ?? ''
    if (!response.ok) {
      let problem: ApiProblem = {}
      try { problem = await response.json() as ApiProblem } catch { /* respuesta sin cuerpo */ }
      if (contentType.includes('application/problem+json') || problem.codigo || problem.detail) throw safeProblem(problem, response.status)
      const code = response.status === 401
        ? 'NO_AUTENTICADO'
        : response.status === 403
          ? 'ACCESO_DENEGADO'
          : response.status === 404
            ? 'RECURSO_NO_ENCONTRADO'
            : 'ERROR_HTTP'
      throw new ApiError(
        code === 'RECURSO_NO_ENCONTRADO'
          ? 'El recurso solicitado no existe o ya no está disponible.'
          : code === 'NO_AUTENTICADO'
            ? 'Tu sesión no es válida. Inicia sesión nuevamente.'
            : code === 'ACCESO_DENEGADO'
              ? 'No tienes permisos para realizar esta operación.'
              : 'No fue posible completar la solicitud.',
        response.status,
        code,
      )
    }
    if (response.status === 204) return undefined as T
    return await response.json() as T
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError('No se pudo conectar con el servicio. Intenta de nuevo más tarde.', undefined, 'ERROR_RED')
  }
}

export function query(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}

export function isSessionError(error: unknown) {
  return error instanceof ApiError && (error.code === 'NO_AUTENTICADO' || error.code === 'SESION_INVALIDA')
}
