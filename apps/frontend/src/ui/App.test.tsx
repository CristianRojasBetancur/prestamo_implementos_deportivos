import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const user = { id: 'user-1', nombres: 'Laura', apellidos: 'Gómez', identificacion: '102334455', activo: true }
const sessionUser = { id: 'provider-1', nombres: 'María', apellidos: 'Rodríguez', correo: 'maria@institucion.edu.co', rol: 'PRESTADOR' }
const category = { id: 'category-1', nombre: 'Balones', activa: true }
const availability = { nombre: 'Balón de fútbol', categoria: category, unidadesDisponibles: 2, unidadesTotales: 3, disponible: true }
const unit = { id: 'unit-1', codigoInventario: 'BAL-001', nombre: 'Balón de fútbol', categoria: category, estado: 'DISPONIBLE', prestable: true, fechaRegistro: '2026-08-31T18:00:00-05:00' }
const loanSummaries = [
  { id: 'loan-1', codigo: 'PRE-000001', estado: 'VIGENTE', usuario: { id: user.id, nombres: user.nombres, apellidos: user.apellidos, identificacion: user.identificacion }, fechaPrestamo: '2026-08-31T18:00:00-05:00', fechaLimiteDevolucion: '2026-09-01T18:00:00-05:00', vencido: false, cantidadImplementos: 1 },
  { id: 'loan-2', codigo: 'PRE-000002', estado: 'DEVOLUCION_PARCIAL', usuario: { id: 'user-2', nombres: 'Carlos', apellidos: 'Pérez', identificacion: '999999' }, fechaPrestamo: '2026-08-30T18:00:00-05:00', fechaLimiteDevolucion: '2026-08-31T18:00:00-05:00', vencido: false, cantidadImplementos: 2 },
]
const loanDetail = { ...loanSummaries[1], prestador: sessionUser, fechaFinalizacion: null, unidadesPendientes: 1, detalles: [{ id: 'detail-2', implemento: unit, estado: 'PENDIENTE_DEVOLUCION' }] }
const createdUser = { id: 'user-2', nombres: 'Carlos', apellidos: 'Pérez', identificacion: '999999', activo: true }
let failUserCreation = false

function json(data: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(data), { status, headers: { 'content-type': 'application/json' } }))
}

function responseFor(url: string, init?: RequestInit) {
  const path = new URL(url).pathname
  if (path.endsWith('/availability')) return json({ contenido: [availability], pagina: 0, tamano: 100, totalElementos: 1, totalPaginas: 1 })
  if (path.endsWith('/categorias')) return json([category, { id: 'category-2', nombre: 'Accesorios', activa: true }])
  if (path.endsWith('/auth/login')) return json({ tokenAcceso: 'token', tipoToken: 'Bearer', expiraEn: '2026-09-01T00:00:00-05:00', usuario: sessionUser })
  if (path.endsWith('/auth/me')) return json(sessionUser)
  if (path.endsWith('/inventory/units') || path.endsWith('/implementos/prestables')) return json({ contenido: [unit], pagina: 0, tamano: 100, totalElementos: 1, totalPaginas: 1, resumen: { total: 1, DISPONIBLE: 1 } })
  if (path.endsWith('/usuarios') && init?.method === 'POST') return failUserCreation
    ? json({ codigo: 'CONFLICTO_UNICIDAD', title: 'Registro duplicado', detail: 'Ya existe un usuario con esa identificación.' }, 409)
    : json(createdUser, 201)
  if (path.endsWith('/usuarios') && new URL(url).searchParams.get('q') === 'NoExiste') return json({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 })
  if (path.endsWith('/usuarios')) return json({ contenido: [user], pagina: 0, tamano: 20, totalElementos: 1, totalPaginas: 1 })
  if (path.endsWith('/usuarios/user-1/situacion')) return json({ usuario: user, habilitado: true, motivos: [], unidadesActivas: 1, unidadesMaximas: 4, unidadesDisponiblesParaPrestar: 3, prestamosActivos: 1, prestamosVencidos: 0, sancionVigente: null })
  if (path.endsWith('/usuarios/user-2/situacion')) return json({ usuario: createdUser, habilitado: true, motivos: [], unidadesActivas: 0, unidadesMaximas: 4, unidadesDisponiblesParaPrestar: 4, prestamosActivos: 0, prestamosVencidos: 0, sancionVigente: null })
  if (path.endsWith('/incidencias')) return json({ contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 0 })
  if (path.endsWith('/devoluciones')) return json({ contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 0 })
  if (path.endsWith('/prestamos/loan-1')) return json({ ...loanSummaries[0], prestador: sessionUser, fechaFinalizacion: null, unidadesPendientes: 1, detalles: [{ id: 'detail-1', implemento: unit, estado: 'PENDIENTE_DEVOLUCION' }] })
  if (path.endsWith('/prestamos/loan-2')) return json(loanDetail)
  if (path.endsWith('/prestamos') && init?.method === 'POST') return json({ id: 'loan-1', codigo: 'PRE-000001', estado: 'VIGENTE', usuario: user, prestador: sessionUser, fechaPrestamo: '2026-08-31T18:00:00-05:00', fechaLimiteDevolucion: '2026-09-01T18:00:00-05:00', fechaFinalizacion: null, vencido: false, unidadesPendientes: 1, detalles: [{ id: 'detail-1', implemento: unit, estado: 'PENDIENTE_DEVOLUCION' }] }, 201)
  if (path.endsWith('/prestamos')) return json({ contenido: loanSummaries, pagina: 0, tamano: 100, totalElementos: loanSummaries.length, totalPaginas: 1 })
  return json({})
}

beforeEach(() => {
  failUserCreation = false
  vi.stubEnv('VITE_API_BASE_URL', 'http://api.test/api/v1')
  sessionStorage.clear()
  vi.stubGlobal('fetch', vi.fn((url: string | URL, init?: RequestInit) => responseFor(String(url), init)))
})

describe('integración de la aplicación con la API', () => {
  it('muestra el catálogo real y filtra los resultados', async () => {
    render(<App />)
    expect(await screen.findByText('Balón de fútbol')).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar implementos' }), { target: { value: 'fútbol' } })
    expect(screen.getByText('Balón de fútbol')).toBeInTheDocument()
  })

  it('inicia sesión con el backend y construye la navegación con el rol recibido', async () => {
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria@institucion.edu.co' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    expect(await screen.findByRole('navigation', { name: 'Navegación principal' })).toBeInTheDocument()
    expect(within(screen.getByRole('navigation', { name: 'Navegación principal' })).getByRole('button', { name: /préstamos/i })).toBeInTheDocument()
    expect(sessionStorage.getItem('prestamos.accessToken')).toBe('token')
  })

  it('consulta un módulo protegido desde la API', async () => {
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    const navigation = await screen.findByRole('navigation', { name: 'Navegación principal' })
    fireEvent.click(within(navigation).getByRole('button', { name: /incidencias/i }))
    expect(await screen.findByText('No hay incidencias')).toBeInTheDocument()
  })

  it('consulta situación y unidades antes de permitir confirmar un préstamo', async () => {
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    const navigation = await screen.findByRole('navigation', { name: 'Navegación principal' })
    fireEvent.click(within(navigation).getByRole('button', { name: /préstamos/i }))
    fireEvent.click(await screen.findByRole('button', { name: /registrar préstamo/i }))
    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar usuario' }), { target: { value: 'Laura' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    fireEvent.click(await screen.findByRole('button', { name: /Laura Gómez/i }))
    expect(await screen.findByText('Usuario habilitado')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /continuar con este usuario/i }))
    expect(await screen.findByText('Paso 2 de 3')).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: /Balón de fútbol/i }))
    fireEvent.click(screen.getByRole('button', { name: 'Continuar' }))
    expect(await screen.findByText('Paso 3 de 3')).toBeInTheDocument()
  })

  it('permite seleccionar el préstamo antes de abrir el formulario de devolución', async () => {
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    const navigation = await screen.findByRole('navigation', { name: 'Navegación principal' })
    fireEvent.click(within(navigation).getByRole('button', { name: /devoluciones/i }))
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar devolución' }))

    expect(await screen.findByText('PRE-000001')).toBeInTheDocument()
    expect(screen.getByText('PRE-000002')).toBeInTheDocument()
    expect(screen.queryByText('No se encontraron préstamos')).not.toBeInTheDocument()

    const selectedRow = screen.getByText('PRE-000002').closest('tr')
    expect(selectedRow).not.toBeNull()
    fireEvent.click(within(selectedRow as HTMLElement).getByRole('button', { name: 'Seleccionar' }))

    expect(await screen.findByText('1 de 1 unidad(es) seleccionada(s)')).toBeInTheDocument()
    expect(screen.getByText('PRE-000002')).toBeInTheDocument()
  })

  it('permite registrar un usuario no encontrado y continuar el préstamo', async () => {
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    const navigation = await screen.findByRole('navigation', { name: 'Navegación principal' })
    fireEvent.click(within(navigation).getByRole('button', { name: /préstamos/i }))
    fireEvent.click(await screen.findByRole('button', { name: /registrar préstamo/i }))
    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar usuario' }), { target: { value: 'NoExiste' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar usuario' }))
    fireEvent.change(screen.getByLabelText('Identificación'), { target: { value: '999999' } })
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Carlos' } })
    fireEvent.change(screen.getByLabelText('Apellidos'), { target: { value: 'Pérez' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear usuario' }))
    expect(await screen.findByText('Paso 2 de 3')).toBeInTheDocument()
    expect(screen.getByText('Carlos Pérez')).toBeInTheDocument()
  })

  it('muestra el conflicto de identificación cuando la API rechaza el alta', async () => {
    failUserCreation = true
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'maria' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secreta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    const navigation = await screen.findByRole('navigation', { name: 'Navegación principal' })
    fireEvent.click(within(navigation).getByRole('button', { name: /préstamos/i }))
    fireEvent.click(await screen.findByRole('button', { name: /registrar préstamo/i }))
    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar usuario' }), { target: { value: 'NoExiste' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar usuario' }))
    fireEvent.change(screen.getByLabelText('Identificación'), { target: { value: '999999' } })
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Carlos' } })
    fireEvent.change(screen.getByLabelText('Apellidos'), { target: { value: 'Pérez' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear usuario' }))
    expect((await screen.findAllByText('Ya existe un registro con esos datos.')).length).toBeGreaterThan(0)
    expect(screen.getByText('Registrar usuario')).toBeInTheDocument()
  })
})
