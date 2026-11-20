import { beforeEach, describe, expect, it, vi } from 'vitest'
import { incidentsApi, inventoryApi, returnsApi, usersApi } from './resources'

beforeEach(() => {
  vi.stubEnv('VITE_API_BASE_URL', 'http://api.test/api/v1')
  sessionStorage.clear()
})

describe('adaptadores de devoluciones e incidencias', () => {
  it('registra un usuario solicitante en el recurso operativo del prestador', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ id: 'user-uuid', activo: true }), {
      status: 201,
      headers: { 'content-type': 'application/json' },
    })))
    vi.stubGlobal('fetch', fetchMock)

    const body = { identificacion: '102334455', nombres: 'Laura', apellidos: 'Gómez' }
    await usersApi.create(body)

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('http://api.test/api/v1/usuarios')
    expect(init.method).toBe('POST')
    expect(JSON.parse(String(init.body))).toEqual(body)
  })

  it('consulta unidades prestables mediante el recurso operativo del prestador', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ contenido: [] }), {
      headers: { 'content-type': 'application/json' },
    })))
    vi.stubGlobal('fetch', fetchMock)

    await inventoryApi.available({ tamano: 100 })

    const [url] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('http://api.test/api/v1/implementos/prestables?pagina=0&tamano=100')
  })

  it('consulta devoluciones en el recurso paginado del backend', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ contenido: [] }), {
      headers: { 'content-type': 'application/json' },
    })))
    vi.stubGlobal('fetch', fetchMock)

    await returnsApi.list({ tamano: 100 })

    const [url] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('http://api.test/api/v1/devoluciones?pagina=0&tamano=100')
  })

  it('registra la devolución usando el UUID del préstamo como subrecurso', async () => {
    const body = { items: [{ implementoId: 'unit-uuid', condicion: 'BUENO' as const }] }
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ id: 'return-uuid' }), {
      headers: { 'content-type': 'application/json' },
    })))
    vi.stubGlobal('fetch', fetchMock)

    await returnsApi.create('loan-uuid', body)

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('http://api.test/api/v1/prestamos/loan-uuid/devoluciones')
    expect(init.method).toBe('POST')
    expect(JSON.parse(String(init.body))).toEqual(body)
  })

  it('consulta incidencias en el recurso autorizado para ADMIN y PRESTADOR', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(JSON.stringify({ contenido: [] }), {
      headers: { 'content-type': 'application/json' },
    })))
    vi.stubGlobal('fetch', fetchMock)

    await incidentsApi.list({ tamano: 100 })

    const [url] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(url).toBe('http://api.test/api/v1/incidencias?pagina=0&tamano=100')
  })
})
