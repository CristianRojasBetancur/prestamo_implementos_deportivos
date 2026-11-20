import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './client'

beforeEach(() => {
  vi.stubEnv('VITE_API_BASE_URL', 'http://api.test/api/v1')
  sessionStorage.clear()
})

describe('cliente HTTP', () => {
  it('envía el token y el identificador de correlación', async () => {
    sessionStorage.setItem('prestamos.accessToken', 'token')
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ ok: true }), { headers: { 'content-type': 'application/json' } })))
    vi.stubGlobal('fetch', fetchMock)
    await request<{ ok: boolean }>('/health')
    const call = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    const headers = call[1]?.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer token')
    expect(headers.get('X-Correlation-Id')).toBeTruthy()
  })

  it('normaliza problem details sin exponer mensajes internos', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(JSON.stringify({ status: 409, codigo: 'CONFLICTO_UNICIDAD', detail: 'constraint interna', errores: [{ campo: 'correo', mensaje: 'ya existe' }] }), { status: 409, headers: { 'content-type': 'application/problem+json' } }))))
    await expect(request('/providers')).rejects.toMatchObject({ code: 'CONFLICTO_UNICIDAD', status: 409, message: 'Ya existe un registro con esos datos.' })
    try { await request('/providers') } catch (error) { expect(error).toBeInstanceOf(ApiError); expect((error as ApiError).message).not.toContain('constraint') }
  })

  it('representa una API no configurada como un error explícito', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '')
    await expect(request('/availability')).rejects.toMatchObject({ code: 'API_NO_CONFIGURADA' })
  })

  it('normaliza un 404 sin mostrar el detalle interno del backend', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response('not found', { status: 404 }))))
    await expect(request('/devoluciones/unknown')).rejects.toMatchObject({
      code: 'RECURSO_NO_ENCONTRADO',
      status: 404,
      message: 'El recurso solicitado no existe o ya no está disponible.',
    })
  })
})
