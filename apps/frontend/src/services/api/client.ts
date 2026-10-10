export class ApiError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message)
    this.name = 'ApiError'
  }
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '')

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  if (!apiBaseUrl) throw new ApiError('La conexión con el servicio aún no está configurada.')
  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...init?.headers },
    })
    if (!response.ok) throw new ApiError('No fue posible completar la solicitud.', response.status)
    return await response.json() as T
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError('No se pudo conectar con el servicio. Intenta de nuevo más tarde.')
  }
}
