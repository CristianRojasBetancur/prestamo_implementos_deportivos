import { useEffect, useMemo, useState } from 'react'
import type React from 'react'
import { ApiError, isSessionError } from '../services/api/client'
import { authApi, catalogApi, incidentsApi, inventoryApi, loansApi, providersApi, returnsApi, usersApi } from '../services/api/resources'
import { clearSession, readSession, saveSession, type StoredSession } from '../services/api/session'
import { ReturnFormPartial } from './ReturnForm'
import type {
  AvailabilityItem, Category, CreateIncidentRequest, CreateUserRequest, Incident, InventoryState, InventoryUnit, Loan, LoanSummary,
  Provider, ReturnSummary, Role, UserSituation, UserSummary,
} from '../services/api/types'

type Page = 'catalog' | 'login' | 'home' | 'inventory' | 'providers' | 'new-item' | 'edit-item' | 'new-provider' | 'edit-provider' | 'loans' | 'loan-detail' | 'loan-1' | 'loan-2' | 'loan-3' | 'returns' | 'return-loan-selection' | 'return-form' | 'incidents' | 'new-incident'
type Notice = { tone: 'info' | 'success' | 'error'; text: string }

const pageTitles: Partial<Record<Page, { title: string; subtitle: string }>> = {
  home: { title: 'Inicio', subtitle: 'Accede rápidamente a las operaciones principales del servicio.' },
  inventory: { title: 'Inventario', subtitle: 'Consulta, busca y administra las unidades físicas del inventario.' },
  providers: { title: 'Prestadores', subtitle: 'Consulta, busca y administra el personal autorizado.' },
  loans: { title: 'Préstamos', subtitle: 'Consulta, filtra y revisa el estado de los préstamos registrados.' },
  returns: { title: 'Devoluciones', subtitle: 'Consulta y registra devoluciones asociadas a préstamos.' },
  incidents: { title: 'Incidencias', subtitle: 'Consulta las novedades registradas sobre implementos prestados.' },
  'new-item': { title: 'Nuevo implemento', subtitle: 'Registra una nueva unidad física en el inventario.' },
  'edit-item': { title: 'Editar implemento', subtitle: 'Actualiza la información de la unidad física.' },
  'new-provider': { title: 'Nuevo prestador', subtitle: 'Registra un nuevo prestador autorizado.' },
  'edit-provider': { title: 'Editar prestador', subtitle: 'Actualiza los datos del prestador autorizado.' },
  'loan-1': { title: 'Registrar préstamo', subtitle: 'Paso 1 de 3 · Identifica y consulta la situación del usuario.' },
  'loan-2': { title: 'Registrar préstamo', subtitle: 'Paso 2 de 3 · Selecciona unidades disponibles.' },
  'loan-3': { title: 'Registrar préstamo', subtitle: 'Paso 3 de 3 · Revisa la información antes de confirmar.' },
  'loan-detail': { title: 'Detalle del préstamo', subtitle: 'Consulta los implementos y el estado de esta entrega.' },
  'return-loan-selection': { title: 'Registrar devolución', subtitle: 'Selecciona el préstamo al que pertenece la devolución.' },
  'return-form': { title: 'Registrar devolución', subtitle: 'Registra la condición de cada implemento y sus novedades.' },
  'new-incident': { title: 'Registrar incidencia', subtitle: 'Registra una pérdida o robo asociado a un préstamo.' },
}

const roleLabel = (role: Role) => role === 'ADMIN' ? 'Administrador' : 'Prestador'
const fullName = (person?: { nombres: string; apellidos: string }) => person ? `${person.nombres} ${person.apellidos}` : '—'
const formatDate = (value?: string | null) => value ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
const displayState = (state: string) => ({ DISPONIBLE: 'Disponible', PRESTADO: 'Prestado', EN_MANTENIMIENTO: 'En mantenimiento', DADO_DE_BAJA: 'Dado de baja', ACTIVO: 'Activo', INACTIVO: 'Inactivo', VIGENTE: 'Activo', DEVOLUCION_PARCIAL: 'Activo', VENCIDO: 'Vencido', FINALIZADO: 'Finalizado', REGISTRADA: 'Registrada', RESUELTA: 'Resuelta', BUENO: 'Buen estado', CON_NOVEDAD: 'Con novedad', PARCIAL: 'Parcial', COMPLETA: 'Completa', DANO: 'Daño', PERDIDA: 'Pérdida', ROBO: 'Robo', OTRA: 'Otra' } as Record<string, string>)[state] ?? state
const toneFor = (state: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' => ['DISPONIBLE', 'ACTIVO', 'VIGENTE', 'DEVOLUCION_PARCIAL', 'BUENO', 'RESUELTA', 'COMPLETA'].includes(state) ? 'success' : ['EN_MANTENIMIENTO', 'PARCIAL'].includes(state) ? 'warning' : ['VENCIDO', 'DADO_DE_BAJA', 'CON_NOVEDAD', 'PERDIDA', 'ROBO', 'DANO'].includes(state) ? 'danger' : ['PRESTADO', 'REGISTRADA', 'FINALIZADO', 'OTRA'].includes(state) ? 'info' : 'neutral'

function errorText(error: unknown) {
  if (error instanceof ApiError) return error.message
  return 'No fue posible completar la operación.'
}

export default function App() {
  const [session, setSession] = useState<StoredSession | null>(() => readSession())
  const [page, setPage] = useState<Page>(() => readSession() ? 'home' : 'catalog')
  const [menuOpen, setMenuOpen] = useState(false)
  const [notice, setNotice] = useState<Notice | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const [catalog, setCatalog] = useState<AvailabilityItem[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [providers, setProviders] = useState<Provider[]>([])
  const [inventory, setInventory] = useState<InventoryUnit[]>([])
  const [inventorySummary, setInventorySummary] = useState<Record<string, number>>({})
  const [users, setUsers] = useState<UserSummary[]>([])
  const [loans, setLoans] = useState<LoanSummary[]>([])
  const [returns, setReturns] = useState<ReturnSummary[]>([])
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [selectedLoan, setSelectedLoan] = useState<Loan | null>(null)
  const [selectedUser, setSelectedUser] = useState<UserSummary | null>(null)
  const [situation, setSituation] = useState<UserSituation | null>(null)
  const [availableUnits, setAvailableUnits] = useState<InventoryUnit[]>([])

  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [selectedUnitIds, setSelectedUnitIds] = useState<string[]>([])
  const [form, setForm] = useState({ code: '', name: '', categoryId: '', description: '', state: 'DISPONIBLE' as InventoryState, nombres: '', apellidos: '', identificacion: '', correo: '', contrasenaInicial: '' })

  const notify = (tone: Notice['tone'], text: string) => setNotice({ tone, text })
  const go = (next: Page) => { setPage(next); setSearch(''); setFilter(''); setError(''); setNotice(null); setMenuOpen(false) }

  useEffect(() => {
    if (!session) return
    let active = true
    authApi.me().then((usuario) => {
      if (active) setSession((current) => current ? { ...current, usuario } : current)
    }).catch((reason) => {
      if (!active) return
      clearSession()
      setSession(null)
      go('login')
      if (!isSessionError(reason)) notify('error', errorText(reason))
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    const load = async () => {
      setBusy(true); setError('')
      try {
        if (page === 'catalog') {
          const [availability, categoryList] = await Promise.all([catalogApi.availability({ tamano: 100 }), catalogApi.categories()])
          if (active) { setCatalog(availability.contenido); setCategories(categoryList) }
        } else if (page === 'providers' && session?.usuario.rol === 'ADMIN') {
          const result = await providersApi.list({ tamano: 100 }); if (active) setProviders(result.contenido)
        } else if (page === 'inventory' && session?.usuario.rol === 'ADMIN') {
          const [result, categoryList] = await Promise.all([inventoryApi.list({ tamano: 100 }), catalogApi.categories()])
          if (active) { setInventory(result.contenido); setInventorySummary(result.resumen ?? {}); setCategories(categoryList) }
        } else if (page === 'loans' && session?.usuario.rol === 'PRESTADOR') {
          const result = await loansApi.list({ tamano: 100 }); if (active) setLoans(result.contenido)
        } else if (page === 'returns' && session?.usuario.rol === 'PRESTADOR') {
          const result = await returnsApi.list({ tamano: 100 }); if (active) setReturns(result.contenido)
        } else if (page === 'incidents' && session) {
          const result = await incidentsApi.list({ tamano: 100 }); if (active) setIncidents(result.contenido)
        }
      } catch (reason) {
        if (active) { setError(errorText(reason)); if (isSessionError(reason)) logout(false) }
      } finally { if (active) setBusy(false) }
    }
    if (page !== 'login' && (page === 'catalog' || session)) void load()
    return () => { active = false }
  }, [page, session?.usuario.id])

  async function login(event: React.FormEvent<HTMLFormElement>, credentials: { identificador: string; contrasena: string }) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const result = await authApi.login(credentials)
      const next = { tokenAcceso: result.tokenAcceso, usuario: result.usuario }
      saveSession(next); setSession(next); go('home'); notify('success', `Sesión iniciada como ${roleLabel(result.usuario.rol)}.`)
    } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }

  async function logout(callApi = true) {
    if (callApi && session) { try { await authApi.logout() } catch { /* se limpia la sesión local aunque el servidor no responda */ } }
    clearSession(); setSession(null); setSelectedLoan(null); go('catalog'); notify('info', 'Sesión cerrada.')
  }

  async function saveProvider(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true)
    try {
      const body = { nombres: form.nombres, apellidos: form.apellidos, identificacion: form.identificacion, correo: form.correo, contrasenaInicial: form.contrasenaInicial }
      const item = formStateId ? await providersApi.update(formStateId, body) : await providersApi.create(body)
      setProviders((current) => formStateId ? current.map((entry) => entry.id === item.id ? item : entry) : [item, ...current])
      go('providers'); notify('success', formStateId ? 'Prestador actualizado.' : 'Prestador creado.')
    } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }

  const [formStateId, setFormStateId] = useState<string | null>(null)
  function editProvider(item: Provider) { setFormStateId(item.id); setForm({ ...form, nombres: item.nombres, apellidos: item.apellidos, identificacion: item.identificacion, correo: item.correo, contrasenaInicial: '' }); go('edit-provider') }
  function newProvider() { setFormStateId(null); setForm({ ...form, nombres: '', apellidos: '', identificacion: '', correo: '', contrasenaInicial: '' }); go('new-provider') }
  async function toggleProvider(item: Provider) { setBusy(true); try { const updated = await providersApi.changeState(item.id, item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'); setProviders((current) => current.map((entry) => entry.id === item.id ? updated : entry)); notify('success', 'Estado del prestador actualizado.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }

  async function saveInventory(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true)
    try {
      const body = { codigoInventario: form.code, nombre: form.name, categoriaId: form.categoryId, descripcion: form.description }
      const item = formStateId ? await inventoryApi.update(formStateId, body) : await inventoryApi.create({ ...body, estado: form.state === 'EN_MANTENIMIENTO' ? 'EN_MANTENIMIENTO' : 'DISPONIBLE' })
      setInventory((current) => formStateId ? current.map((entry) => entry.id === item.id ? item : entry) : [item, ...current])
      go('inventory'); notify('success', formStateId ? 'Implemento actualizado.' : 'Implemento creado.')
    } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }
  function editInventory(item: InventoryUnit) { setFormStateId(item.id); setForm({ ...form, code: item.codigoInventario, name: item.nombre, categoryId: item.categoria.id, description: item.descripcion ?? '', state: item.estado }); go('edit-item') }
  function newInventory() { setFormStateId(null); setForm({ ...form, code: '', name: '', categoryId: categories[0]?.id ?? '', description: '', state: 'DISPONIBLE' }); go('new-item') }
  async function changeInventoryState(item: InventoryUnit, next: Extract<InventoryState, 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'DADO_DE_BAJA'>) {
    const motivo = window.prompt('Motivo del cambio de estado')
    if (!motivo) return
    setBusy(true); try { const updated = await inventoryApi.changeState(item.id, { estado: next, motivo }); setInventory((current) => current.map((entry) => entry.id === item.id ? updated : entry)); notify('success', 'Estado del inventario actualizado.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }

  async function findUsers(value: string) { setBusy(true); setError(''); setUsers([]); setSelectedUser(null); setSituation(null); try { const result = await usersApi.list(value); setUsers(result.contenido) } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }
  async function selectUser(user: UserSummary) { setBusy(true); try { setSelectedUser(user); setSituation(await usersApi.situation(user.id)) } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }
  async function loadAvailableUnits() { setBusy(true); try { setAvailableUnits((await inventoryApi.available({ tamano: 100 })).contenido) } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }
  async function registerUser(body: CreateUserRequest) {
    setBusy(true); setError('')
    try {
      const created = await usersApi.create(body)
      const createdSituation = await usersApi.situation(created.id)
      setUsers([created]); setSelectedUser(created); setSituation(createdSituation)
      if (!createdSituation.habilitado) { go('loan-1'); setError(createdSituation.motivos.map((reason) => reason.mensaje).join(' ')); return }
      go('loan-2')
      const available = await inventoryApi.available({ tamano: 100 })
      setAvailableUnits(available.contenido)
      notify('success', 'Usuario creado y seleccionado para el préstamo.')
    } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }
  async function createLoan() { if (!selectedUser || !selectedUnitIds.length) return; setBusy(true); try { const created = await loansApi.create({ usuarioId: selectedUser.id, implementoIds: selectedUnitIds }); setLoans((current) => [created, ...current]); setSelectedLoan(created); go('loan-detail'); notify('success', 'Préstamo registrado correctamente.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }
  async function openLoan(id: string) { setBusy(true); try { setSelectedLoan(await loansApi.get(id)); go('loan-detail') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }
  async function refreshLoan() { if (selectedLoan) setSelectedLoan(await loansApi.get(selectedLoan.id)) }
  async function startReturn() {
    setBusy(true)
    try {
      const result = await loansApi.list({ tamano: 100, estado: 'VIGENTE,DEVOLUCION_PARCIAL,VENCIDO' })
      setLoans(result.contenido)
      setSelectedLoan(null)
      go('return-loan-selection')
    } catch (reason) { setError(errorText(reason))
    } finally { setBusy(false) }
  }
  async function startReturnForLoan(id: string) {
    setBusy(true); setError('')
    try { setSelectedLoan(await loansApi.get(id)); go('return-form') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) }
  }
  async function startIncident() {
    setBusy(true)
    try {
      const result = await loansApi.list({ tamano: 100 })
      setLoans(result.contenido)
      go('new-incident')
    } catch (reason) { setError(errorText(reason))
    } finally { setBusy(false) }
  }

  const nav = session?.usuario.rol === 'ADMIN' ? [
    ['Inicio', 'home', '⌂'], ['Prestadores', 'providers', '♙'], ['Inventario', 'inventory', '▦'], ['Incidencias', 'incidents', '!'],
  ] as [string, Page, string][] : [['Inicio', 'home', '⌂'], ['Préstamos', 'loans', '↗'], ['Devoluciones', 'returns', '↙'], ['Incidencias', 'incidents', '!']] as [string, Page, string][]
  const title = pageTitles[page]

  if (page === 'catalog') return <PublicCatalog items={catalog} categories={categories} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} loading={busy} error={error} onLogin={() => go('login')} />
  if (page === 'login' || !session) return <Login busy={busy} error={error} onSubmit={login} onBack={() => go('catalog')} />

  return <div className="prototype-app">
    <aside className={`app-sidebar ${menuOpen ? 'open' : ''}`}>
      <div className="sidebar-brand"><strong>Préstamos deportivos</strong><span>{roleLabel(session.usuario.rol)}</span></div>
      <nav aria-label="Navegación principal" className="app-nav">{nav.map(([label, target, icon]) => <button key={target} className={page === target || page.startsWith(`${target}-`) ? 'selected' : ''} onClick={() => go(target)}><i>{icon}</i>{label}</button>)}</nav>
      <div className="sidebar-session"><strong>{fullName(session.usuario)}</strong><span>{session.usuario.correo ?? roleLabel(session.usuario.rol)}</span></div>
    </aside>
    {menuOpen && <button className="sidebar-scrim" aria-label="Cerrar navegación" onClick={() => setMenuOpen(false)} />}
    <div className="prototype-main">
      <header className="app-topbar"><button className="menu-toggle" aria-label="Abrir navegación" onClick={() => setMenuOpen(!menuOpen)}>☰</button><span>Sistema de Préstamo de Implementos Deportivos</span><div className="session"><i /> <span><strong>{fullName(session.usuario)}</strong><small>{roleLabel(session.usuario.rol)}</small></span><button className="top-role" onClick={() => void logout()}>Salir</button></div></header>
      <main className="app-content">
        {title && <PageHeading title={title.title} subtitle={title.subtitle} />}
        {page === 'home' && <Home role={session.usuario.rol} go={go} />}
        {page === 'inventory' && <Inventory items={inventory} summary={inventorySummary} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} busy={busy} onNew={newInventory} onEdit={editInventory} onChangeState={changeInventoryState} />}
        {page === 'providers' && <Providers items={providers} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} busy={busy} onNew={newProvider} onEdit={editProvider} onToggle={toggleProvider} />}
        {(page === 'new-item' || page === 'edit-item') && <InventoryForm form={form} categories={categories} editing={Boolean(formStateId)} setForm={setForm} busy={busy} error={error} onSubmit={saveInventory} onCancel={() => go('inventory')} />}
        {(page === 'new-provider' || page === 'edit-provider') && <ProviderForm form={form} editing={Boolean(formStateId)} setForm={setForm} busy={busy} error={error} onSubmit={saveProvider} onCancel={() => go('providers')} />}
        {page === 'loans' && <Loans items={loans} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} onDetails={openLoan} onNew={() => { setSelectedUser(null); setSituation(null); setSelectedUnitIds([]); go('loan-1') }} />}
        {page === 'loan-detail' && selectedLoan && <LoanDetail loan={selectedLoan} role={session.usuario.rol} onRefresh={refreshLoan} onReturn={() => go('return-form')} onBack={() => go('loans')} />}
        {page === 'loan-1' && <LoanWizardWithCreation step={1} users={users} selectedUser={selectedUser} situation={situation} selectedIds={selectedUnitIds} onFind={findUsers} onSelectUser={selectUser} onRegisterUser={registerUser} error={error} busy={busy} onStep={(step) => { if (step === 2) void loadAvailableUnits(); go(`loan-${step}` as Page) }} />}
        {page === 'loan-2' && <LoanWizard step={2} users={users} selectedUser={selectedUser} situation={situation} units={availableUnits} selectedIds={selectedUnitIds} onToggle={(id) => setSelectedUnitIds((current) => current.includes(id) ? current.filter((entry) => entry !== id) : [...current, id])} onStep={(step) => go(`loan-${step}` as Page)} />}
        {page === 'loan-3' && <LoanWizard step={3} users={users} selectedUser={selectedUser} situation={situation} units={availableUnits} selectedIds={selectedUnitIds} onStep={(step) => go(`loan-${step}` as Page)} onFinish={createLoan} busy={busy} />}
        {page === 'returns' && <Returns items={returns} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} onNew={() => void startReturn()} />}
        {page === 'return-loan-selection' && <ReturnLoanSelection items={loans} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} busy={busy} onSelect={(id) => void startReturnForLoan(id)} onCancel={() => go('returns')} />}
        {page === 'return-form' && (selectedLoan ? <ReturnFormPartial loan={selectedLoan} busy={busy} error={error} onCancel={() => go('returns')} onSubmit={async (body) => { setBusy(true); try { await returnsApi.create(selectedLoan.id, body); await refreshLoan(); go('returns'); notify('success', 'Devolución registrada correctamente.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }} /> : <DataPanel><EmptyCard title="No hay préstamos pendientes" detail="La API debe devolver un préstamo con unidades pendientes antes de registrar una devolución." /><button className="secondary-button" onClick={() => go('loans')}>Volver a préstamos</button></DataPanel>)}
        {page === 'incidents' && <Incidents items={incidents} search={search} setSearch={setSearch} filter={filter} setFilter={setFilter} role={session.usuario.rol} onNew={() => void startIncident()} onResolve={async (id) => { setBusy(true); try { const updated = await incidentsApi.resolve(id); setIncidents((current) => current.map((item) => item.id === id ? updated : item)); notify('success', 'Incidencia resuelta.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }} />}
        {page === 'new-incident' && <IncidentForm loans={loans} busy={busy} error={error} onCancel={() => go('incidents')} onSubmit={async (loanId, body) => { setBusy(true); try { const created = await incidentsApi.create(loanId, body); setIncidents((current) => [created, ...current]); go('incidents'); notify('success', 'Incidencia registrada correctamente.') } catch (reason) { setError(errorText(reason)) } finally { setBusy(false) } }} />}
        {error && <div className="error-strip" role="alert"><strong>La operación no se completó.</strong><span>{error}</span><button className="table-action" onClick={() => setError('')}>Cerrar</button></div>}
        {notice && <div className={`${notice.tone === 'error' ? 'error-strip' : notice.tone === 'success' ? 'success-strip' : 'info-strip'} toast`} role="status">{notice.text}<button onClick={() => setNotice(null)} aria-label="Cerrar mensaje">×</button></div>}
      </main>
    </div>
  </div>
}

function PublicCatalog({ items, categories, search, setSearch, filter, setFilter, loading, error, onLogin }: { items: AvailabilityItem[]; categories: Category[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; loading: boolean; error: string; onLogin: () => void }) {
  const filtered = useMemo(() => items.filter((item) => (!filter || item.categoria.id === filter) && `${item.nombre} ${item.categoria.nombre}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))), [items, filter, search])
  return <main className="public-page"><header className="public-header"><div className="institution-logo">GD</div><span><strong>Gestión Deportiva</strong><small>Institución académica</small></span><button onClick={onLogin}>Acceso institucional</button></header><section className="public-content"><h1>Implementos deportivos</h1><p className="page-subtitle">Consulta la disponibilidad actual antes de dirigirte al punto de préstamo.</p><div className="catalog-filters"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar implementos" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar implementos" /></label><label className="select-wrap"><span>Categoría</span><select aria-label="Categoría" value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todas las categorías</option>{categories.filter((item) => item.activa !== false).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label></div>{loading && <p className="info-strip" role="status">Consultando disponibilidad…</p>}{error && <p className="error-strip" role="alert">{error}</p>}{!loading && !error && <><p className="record-count">{filtered.length} implementos disponibles</p>{filtered.length ? <div className="equipment-grid">{filtered.map((item) => <article className="equipment-card" key={`${item.categoria.id}-${item.nombre}`}><h2>{item.nombre}</h2><p>{item.categoria.nombre} · {item.unidadesDisponibles} de {item.unidadesTotales} disponibles</p><Badge tone={item.disponible ? 'success' : 'neutral'}>{item.disponible ? 'Disponible' : 'No disponible'}</Badge></article>)}</div> : <EmptyCard title="No existen implementos disponibles" detail="Prueba con otro término o categoría." />}</>}</section></main>
}

function Login({ busy, error, onSubmit, onBack }: { busy: boolean; error: string; onSubmit: (event: React.FormEvent<HTMLFormElement>, credentials: { identificador: string; contrasena: string }) => void; onBack: () => void }) {
  const [identificador, setIdentificador] = useState(''); const [contrasena, setContrasena] = useState('')
  return <main className="login-page"><button className="back-link" onClick={onBack}>← Volver al catálogo</button><form className="login-card" onSubmit={(event) => onSubmit(event, { identificador, contrasena })}><div className="institution-logo">GD</div><div className="institution-name"><strong>Gestión Deportiva</strong><small>Institución académica</small></div><h1>Iniciar sesión</h1><p className="login-intro">Ingresa tus credenciales para acceder a las funciones correspondientes a tu rol.</p><label className="form-field">Correo o identificación<input required value={identificador} onChange={(event) => setIdentificador(event.target.value)} /></label><label className="form-field">Contraseña<input required type="password" value={contrasena} onChange={(event) => setContrasena(event.target.value)} /></label>{error && <p className="field-error" role="alert">{error}</p>}<button className="primary-button full-width" disabled={busy}>{busy ? 'Conectando…' : 'Iniciar sesión'}</button><p className="login-foot">La autorización y el rol se validan en el servicio.</p></form></main>
}

function PageHeading({ title, subtitle }: { title: string; subtitle: string }) { return <div className="page-heading"><h1>{title}</h1><p>{subtitle}</p></div> }
function Badge({ tone, children }: { tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral'; children: React.ReactNode }) { return <span className={`badge ${tone}`}>{children}</span> }
function EmptyCard({ title, detail }: { title: string; detail: string }) { return <div className="empty-card"><span>−</span><strong>{title}</strong><p>{detail}</p></div> }
function DataPanel({ children, className = '' }: { children: React.ReactNode; className?: string }) { return <section className={`data-panel ${className}`}>{children}</section> }
function SummaryCard({ label, text }: { label: string; text: string }) { return <article className="summary-card"><strong>{label}</strong><span>{text}</span></article> }
function Home({ role, go }: { role: Role; go: (page: Page) => void }) { return <DataPanel className="home-panel"><h2>Operaciones disponibles</h2><div className="action-grid"><ActionCard title={role === 'ADMIN' ? 'Gestionar prestadores' : 'Registrar préstamo'} text={role === 'ADMIN' ? 'Consulta, actualiza y cambia el estado del personal autorizado.' : 'Verifica la situación del usuario y registra una entrega.'} button="Abrir módulo" primary onClick={() => go(role === 'ADMIN' ? 'providers' : 'loans')} /><ActionCard title={role === 'ADMIN' ? 'Gestionar inventario' : 'Registrar devolución'} text={role === 'ADMIN' ? 'Administra unidades físicas y sus transiciones autorizadas.' : 'Registra condiciones, novedades y sanciones según la respuesta de la API.'} button="Abrir módulo" onClick={() => go(role === 'ADMIN' ? 'inventory' : 'returns')} /></div></DataPanel> }
function ActionCard({ title, text, button, onClick, primary = false }: { title: string; text: string; button: string; onClick: () => void; primary?: boolean }) { return <article className="action-card"><strong>{title}</strong><p>{text}</p><button className={primary ? 'primary-button' : 'secondary-button'} onClick={onClick}>{button}</button></article> }

function Inventory({ items, summary, search, setSearch, filter, setFilter, busy, onNew, onEdit, onChangeState }: { items: InventoryUnit[]; summary: Record<string, number>; search: string; setSearch: (value: string) => void; setFilter: (value: string) => void; filter: string; busy: boolean; onNew: () => void; onEdit: (item: InventoryUnit) => void; onChangeState: (item: InventoryUnit, state: Extract<InventoryState, 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'DADO_DE_BAJA'>) => void }) {
  const filtered = items.filter((item) => (!filter || item.estado === filter) && `${item.codigoInventario} ${item.nombre}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar implemento" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código o nombre" /></label><label className="select-wrap"><span>Estado</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option>{['DISPONIBLE', 'PRESTADO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA'].map((state) => <option key={state} value={state}>{displayState(state)}</option>)}</select></label><button className="primary-button nowrap" onClick={onNew}>＋ Nuevo implemento</button></div><div className="metric-strip"><strong>{items.length} implementos encontrados</strong><span>{Object.entries(summary).filter(([key]) => key !== 'total').map(([key, value]) => `${displayState(key)}: ${value}`).join(' · ')}</span></div><div className="table-scroll"><table><thead><tr><th>Código</th><th>Nombre</th><th>Categoría</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.codigoInventario}</td><td className="strong-cell">{item.nombre}</td><td>{item.categoria.nombre}</td><td><Badge tone={toneFor(item.estado)}>{displayState(item.estado)}</Badge></td><td className="actions-cell"><button className="table-action" onClick={() => onEdit(item)}>Editar</button>{item.estado !== 'PRESTADO' && item.estado !== 'DADO_DE_BAJA' && <label className="select-wrap"><span className="sr-only">Cambiar estado</span><select aria-label={`Cambiar estado ${item.codigoInventario}`} disabled={busy} value={item.estado} onChange={(event) => onChangeState(item, event.target.value as Extract<InventoryState, 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'DADO_DE_BAJA'>)}><option value="DISPONIBLE">Disponible</option><option value="EN_MANTENIMIENTO">Mantenimiento</option><option value="DADO_DE_BAJA">Dar de baja</option></select></label>}</td></tr>)}</tbody></table></div>{!filtered.length && <EmptyCard title="No hay implementos" detail="La API no devolvió unidades para estos filtros." />}</DataPanel>
}

function Providers({ items, search, setSearch, filter, setFilter, busy, onNew, onEdit, onToggle }: { items: Provider[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; busy: boolean; onNew: () => void; onEdit: (item: Provider) => void; onToggle: (item: Provider) => void }) {
  const filtered = items.filter((item) => (!filter || item.estado === filter) && `${item.nombres} ${item.apellidos} ${item.identificacion} ${item.correo}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar prestador" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nombre, identificación o correo" /></label><label className="select-wrap"><span>Estado</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option><option value="ACTIVO">Activo</option><option value="INACTIVO">Inactivo</option></select></label><button className="primary-button nowrap" onClick={onNew}>＋ Nuevo prestador</button></div><div className="metric-strip"><strong>{items.length} prestadores encontrados</strong><span>{items.filter((item) => item.estado === 'ACTIVO').length} activos · {items.filter((item) => item.estado === 'INACTIVO').length} inactivos</span></div><div className="table-scroll"><table><thead><tr><th>Nombre</th><th>Identificación</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td className="strong-cell">{fullName(item)}</td><td>{item.identificacion}</td><td>{item.correo}</td><td><Badge tone={item.estado === 'ACTIVO' ? 'success' : 'neutral'}>{displayState(item.estado)}</Badge></td><td className="actions-cell"><button className="table-action" onClick={() => onEdit(item)}>Editar</button><button disabled={busy} className="danger-button compact" onClick={() => onToggle(item)}>{item.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table></div>{!filtered.length && <EmptyCard title="No hay prestadores" detail="La API no devolvió resultados para estos filtros." />}</DataPanel>
}

function InventoryForm({ form, categories, editing, setForm, busy, error, onSubmit, onCancel }: { form: { code: string; name: string; categoryId: string; description: string; state: InventoryState }; categories: Category[]; editing: boolean; setForm: React.Dispatch<React.SetStateAction<{ code: string; name: string; categoryId: string; description: string; state: InventoryState; nombres: string; apellidos: string; identificacion: string; correo: string; contrasenaInicial: string }>>; busy: boolean; error: string; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; onCancel: () => void }) { return <DataPanel className="form-panel"><form className="entity-form" onSubmit={onSubmit}><h2>{editing ? 'Actualizar implemento' : 'Datos del nuevo implemento'}</h2><p>El estado solo se cambia mediante la operación autorizada de inventario.</p><div className="form-grid"><label className="form-field">Código de inventario<input required value={form.code} onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))} /></label><label className="form-field">Nombre<input required value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} /></label><label className="form-field">Categoría<select required value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}><option value="">Selecciona una categoría</option>{categories.filter((item) => item.activa !== false).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>{!editing && <label className="form-field">Estado inicial<select value={form.state} onChange={(event) => setForm((current) => ({ ...current, state: event.target.value as InventoryState }))}><option value="DISPONIBLE">Disponible</option><option value="EN_MANTENIMIENTO">En mantenimiento</option></select></label>}<label className="form-field full-span">Descripción<textarea value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} rows={3} /></label></div>{error && <p className="field-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button disabled={busy} className="primary-button">{busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear implemento'}</button></div></form></DataPanel> }

function ProviderForm({ form, editing, setForm, busy, error, onSubmit, onCancel }: { form: { nombres: string; apellidos: string; identificacion: string; correo: string; contrasenaInicial: string }; editing: boolean; setForm: React.Dispatch<React.SetStateAction<{ code: string; name: string; categoryId: string; description: string; state: InventoryState; nombres: string; apellidos: string; identificacion: string; correo: string; contrasenaInicial: string }>>; busy: boolean; error: string; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; onCancel: () => void }) { return <DataPanel className="form-panel"><form className="entity-form" onSubmit={onSubmit}><h2>{editing ? 'Actualizar prestador' : 'Datos del nuevo prestador'}</h2><p>La contraseña inicial solo se envía al crear el prestador.</p><div className="form-grid"><label className="form-field">Nombres<input required value={form.nombres} onChange={(event) => setForm((current) => ({ ...current, nombres: event.target.value }))} /></label><label className="form-field">Apellidos<input required value={form.apellidos} onChange={(event) => setForm((current) => ({ ...current, apellidos: event.target.value }))} /></label><label className="form-field">Identificación<input required value={form.identificacion} onChange={(event) => setForm((current) => ({ ...current, identificacion: event.target.value }))} /></label><label className="form-field">Correo institucional<input required type="email" value={form.correo} onChange={(event) => setForm((current) => ({ ...current, correo: event.target.value }))} /></label>{!editing && <label className="form-field full-span">Contraseña inicial<input required minLength={8} type="password" value={form.contrasenaInicial} onChange={(event) => setForm((current) => ({ ...current, contrasenaInicial: event.target.value }))} /></label>}</div>{error && <p className="field-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button disabled={busy} className="primary-button">{busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear prestador'}</button></div></form></DataPanel> }

function Loans({ items, search, setSearch, filter, setFilter, onDetails, onNew }: { items: LoanSummary[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; onDetails: (id: string) => void; onNew: () => void }) { const filtered = items.filter((item) => (!filter || item.estado === filter) && `${item.codigo} ${fullName(item.usuario)} ${item.usuario.identificacion ?? ''}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))); return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar préstamo" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código o usuario" /></label><label className="select-wrap"><span>Estado</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option>{['VIGENTE', 'DEVOLUCION_PARCIAL', 'VENCIDO', 'FINALIZADO'].map((state) => <option key={state} value={state}>{displayState(state)}</option>)}</select></label><button className="primary-button nowrap" onClick={onNew}>Registrar préstamo</button></div><div className="metric-strip"><strong>{items.length} préstamos encontrados</strong><span>Los estados y fechas provienen de la API.</span></div><div className="table-scroll"><table><thead><tr><th>Código</th><th>Usuario</th><th>Implementos</th><th>Fecha préstamo</th><th>Fecha límite</th><th>Estado</th><th /></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.codigo}</td><td className="strong-cell">{fullName(item.usuario)}</td><td>{item.cantidadImplementos}</td><td>{formatDate(item.fechaPrestamo)}</td><td className={item.vencido ? 'overdue' : ''}>{formatDate(item.fechaLimiteDevolucion)}</td><td><Badge tone={toneFor(item.estado)}>{displayState(item.estado)}</Badge></td><td><button className="table-action" onClick={() => onDetails(item.id)}>Ver detalle</button></td></tr>)}</tbody></table></div>{!filtered.length && <EmptyCard title="No hay préstamos" detail="La API no devolvió registros para estos filtros." />}</DataPanel> }

function LoanWizard({ step, users, selectedUser, situation, units = [], selectedIds, onFind, onSelectUser, onToggle, onStep, onFinish, busy = false }: { step: number; users: UserSummary[]; selectedUser: UserSummary | null; situation: UserSituation | null; units?: InventoryUnit[]; selectedIds: string[]; onFind?: (value: string) => void; onSelectUser?: (user: UserSummary) => void; onToggle?: (id: string) => void; onStep: (step: number) => void; onFinish?: () => void; busy?: boolean }) { const [term, setTerm] = useState(''); const max = situation?.unidadesDisponiblesParaPrestar ?? 0; return <DataPanel className="wizard-panel"><div className="step-intro"><Badge tone="info">Paso {step} de 3</Badge><span>La API valida nuevamente la elegibilidad y disponibilidad al confirmar.</span></div>{step === 1 && <><div className="filters-row"><label className="search-input wizard-search"><span className="search-glyph">⌕</span><input aria-label="Buscar usuario" value={term} onChange={(event) => setTerm(event.target.value)} placeholder="Buscar por nombre o identificación" /></label><button className="secondary-button" onClick={() => onFind?.(term)}>Buscar</button></div>{users.map((user) => <button type="button" className={`user-status ${selectedUser?.id === user.id ? 'checked' : ''}`} key={user.id} onClick={() => onSelectUser?.(user)}><div><strong>{fullName(user)}</strong><span>Identificación · {user.identificacion}</span></div><Badge tone={selectedUser?.id === user.id ? 'success' : 'neutral'}>{selectedUser?.id === user.id ? situation?.habilitado ? 'Habilitado' : 'No habilitado' : 'Seleccionar'}</Badge></button>)}{situation && <div className={situation.habilitado ? 'success-strip' : 'error-strip'}><strong>{situation.habilitado ? 'Usuario habilitado' : 'Usuario no habilitado'}</strong><span>{situation.habilitado ? `Puede recibir hasta ${situation.unidadesDisponiblesParaPrestar} implemento(s).` : situation.motivos.map((reason) => reason.mensaje).join(' ')}</span></div>}<div className="wizard-actions"><button disabled={!selectedUser || !situation?.habilitado} className="primary-button" onClick={() => onStep(2)}>Continuar con este usuario</button></div></>}{step === 2 && <><div className="user-status compact-status"><strong>{fullName(selectedUser ?? undefined)}</strong><span>{situation?.unidadesActivas ?? 0} activos · {situation?.prestamosVencidos ?? 0} vencidos</span><Badge tone="success">Máximo {max}</Badge></div><div className="selectable-grid">{units.filter((unit) => !selectedIds.includes(unit.id) || selectedIds.includes(unit.id)).map((unit) => <button type="button" className={`selectable-card ${selectedIds.includes(unit.id) ? 'checked' : ''}`} key={unit.id} onClick={() => onToggle?.(unit.id)} disabled={!selectedIds.includes(unit.id) && selectedIds.length >= max}><strong>{unit.nombre}</strong><span>{unit.categoria.nombre} · {unit.codigoInventario}</span><Badge tone="success">Disponible</Badge></button>)}</div>{!units.length && <EmptyCard title="No hay unidades disponibles" detail="La API no devolvió implementos seleccionables." />}<div className="metric-strip"><strong>{selectedIds.length} seleccionados</strong><span>El límite autoritativo lo determina la situación del usuario.</span></div><div className="wizard-actions"><button className="secondary-button" onClick={() => onStep(1)}>Volver</button><button disabled={!selectedIds.length} className="primary-button" onClick={() => onStep(3)}>Continuar</button></div></>}{step === 3 && <><div className="user-status compact-status"><strong>{fullName(selectedUser ?? undefined)}</strong><span>{selectedIds.length} unidad(es) seleccionada(s)</span><Badge tone="success">Revisión</Badge></div><div className="confirm-grid"><div className="summary-card"><strong>Unidades seleccionadas</strong>{selectedIds.map((id) => <span key={id}>{units.find((unit) => unit.id === id)?.nombre ?? id}</span>)}</div><div className="summary-card"><strong>Fechas</strong><span>Las fechas de entrega y devolución se calculan en el servidor.</span><Badge tone="warning">Duración regular: 24 horas</Badge></div></div><div className="warning-strip">Al confirmar, el backend realizará la transacción completa y actualizará disponibilidad.</div><div className="wizard-actions"><button className="secondary-button" onClick={() => onStep(2)}>Volver</button><button disabled={busy} className="primary-button" onClick={onFinish}>{busy ? 'Registrando…' : 'Confirmar préstamo'}</button></div></>}</DataPanel> }

type LoanWizardCreationProps = {
  step: number
  users: UserSummary[]
  selectedUser: UserSummary | null
  situation: UserSituation | null
  units?: InventoryUnit[]
  selectedIds: string[]
  onFind?: (value: string) => void
  onSelectUser?: (user: UserSummary) => void
  onToggle?: (id: string) => void
  onRegisterUser?: (body: CreateUserRequest) => Promise<void>
  onStep: (step: number) => void
  onFinish?: () => void
  busy?: boolean
  error?: string
}

function LoanWizardWithCreation(props: LoanWizardCreationProps) {
  if (props.step !== 1) return <LoanWizard {...props} />
  return <LoanWizardStepOne {...props} />
}

function LoanWizardStepOne({ users, selectedUser, situation, onFind, onSelectUser, onRegisterUser, onStep, busy = false, error = '' }: LoanWizardCreationProps) {
  const [term, setTerm] = useState('')
  const [searched, setSearched] = useState(false)
  const [creating, setCreating] = useState(false)

  if (creating) {
    return <UserCreateForm
      initialIdentification={/^\d+$/.test(term.trim()) ? term.trim() : ''}
      busy={busy}
      error={error}
      onCancel={() => setCreating(false)}
      onSubmit={async (body) => { await onRegisterUser?.(body) }}
    />
  }

  const canRegister = searched && !busy && !error && !users.length && term.trim().length > 0
  return <DataPanel className="wizard-panel">
    <div className="step-intro"><Badge tone="info">Paso 1 de 3</Badge><span>Busca al usuario por nombre o identificación.</span></div>
    <div className="filters-row">
      <label className="search-input wizard-search"><span className="search-glyph">⌕</span><input aria-label="Buscar usuario" value={term} onChange={(event) => { setTerm(event.target.value); setSearched(false) }} placeholder="Buscar por nombre o identificación" /></label>
      <button disabled={busy || !term.trim()} className="secondary-button" onClick={() => { setSearched(true); void onFind?.(term) }}>{busy ? 'Buscando…' : 'Buscar'}</button>
    </div>
    {users.map((user) => <button type="button" className={`user-status ${selectedUser?.id === user.id ? 'checked' : ''}`} key={user.id} onClick={() => onSelectUser?.(user)}><div><strong>{fullName(user)}</strong><span>Identificación · {user.identificacion}</span></div><Badge tone={selectedUser?.id === user.id ? 'success' : 'neutral'}>{selectedUser?.id === user.id ? situation?.habilitado ? 'Habilitado' : 'No habilitado' : 'Seleccionar'}</Badge></button>)}
    {canRegister && <div className="empty-card"><span>⌕</span><strong>No encontramos un usuario</strong><p>Registra sus datos para continuar con el préstamo.</p><button className="primary-button" onClick={() => setCreating(true)}>Registrar usuario</button></div>}
    {situation && <div className={situation.habilitado ? 'success-strip' : 'error-strip'}><strong>{situation.habilitado ? 'Usuario habilitado' : 'Usuario no habilitado'}</strong><span>{situation.habilitado ? `Puede recibir hasta ${situation.unidadesDisponiblesParaPrestar} implemento(s).` : situation.motivos.map((reason) => reason.mensaje).join(' ')}</span></div>}
    <div className="wizard-actions"><button disabled={!selectedUser || !situation?.habilitado} className="primary-button" onClick={() => onStep(2)}>Continuar con este usuario</button></div>
  </DataPanel>
}

function UserCreateForm({ initialIdentification, busy, error, onCancel, onSubmit }: { initialIdentification: string; busy: boolean; error: string; onCancel: () => void; onSubmit: (body: CreateUserRequest) => Promise<void> }) {
  const [form, setForm] = useState<CreateUserRequest>({ identificacion: initialIdentification, nombres: '', apellidos: '' })
  return <DataPanel className="form-panel"><form className="entity-form" onSubmit={(event) => { event.preventDefault(); void onSubmit(form) }}><h2>Registrar usuario</h2><p>Completa los datos del solicitante para continuar con el préstamo.</p><div className="form-grid"><label className="form-field">Identificación<input required maxLength={40} value={form.identificacion} onChange={(event) => setForm((current) => ({ ...current, identificacion: event.target.value }))} /></label><label className="form-field">Nombres<input required maxLength={120} value={form.nombres} onChange={(event) => setForm((current) => ({ ...current, nombres: event.target.value }))} /></label><label className="form-field">Apellidos<input required maxLength={120} value={form.apellidos} onChange={(event) => setForm((current) => ({ ...current, apellidos: event.target.value }))} /></label></div>{error && <p className="field-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Volver a la búsqueda</button><button disabled={busy} className="primary-button">{busy ? 'Registrando…' : 'Crear usuario'}</button></div></form></DataPanel>
}

function LoanDetail({ loan, role, onReturn, onBack, onRefresh }: { loan: Loan; role: Role; onReturn: () => void; onBack: () => void; onRefresh: () => Promise<void> }) { return <DataPanel className="detail-panel"><div className="metric-strip"><strong>{loan.codigo}</strong><Badge tone={toneFor(loan.estado)}>{displayState(loan.estado)}</Badge><span>{fullName(loan.usuario)} · {loan.unidadesPendientes} pendientes</span></div><div className="confirm-grid"><div className="summary-card"><strong>Implementos del préstamo</strong>{loan.detalles.map((detail) => <span key={detail.id}>{detail.implemento.nombre} · {detail.implemento.codigoInventario} <Badge tone={toneFor(detail.estado)}>{displayState(detail.estado)}</Badge></span>)}</div><div className="summary-card"><strong>Fechas</strong><span>Entrega <b>{formatDate(loan.fechaPrestamo)}</b></span><span>Límite <b>{formatDate(loan.fechaLimiteDevolucion)}</b></span></div></div><div className="summary-grid three"><SummaryCard label="Devoluciones" text={`${loan.devoluciones?.length ?? 0} registradas`} /><SummaryCard label="Incidencias" text={`${loan.incidencias?.length ?? 0} asociadas`} /><SummaryCard label="Estado operativo" text={loan.unidadesPendientes ? 'Aún hay unidades pendientes.' : 'Todas las unidades fueron resueltas.'} /></div><div className="wizard-actions"><button className="secondary-button" onClick={onBack}>Volver</button>{role === 'PRESTADOR' && loan.unidadesPendientes > 0 && <button className="primary-button" onClick={onReturn}>Registrar devolución</button>}<button className="secondary-button" onClick={() => void onRefresh()}>Actualizar</button></div></DataPanel> }

function ReturnLoanSelection({ items, search, setSearch, filter, setFilter, busy, onSelect, onCancel }: { items: LoanSummary[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; busy: boolean; onSelect: (id: string) => void; onCancel: () => void }) {
  const filtered = items.filter((item) => item.estado !== 'FINALIZADO' && (!filter || item.estado === filter) && `${item.codigo} ${fullName(item.usuario)} ${item.usuario.identificacion ?? ''}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')))
  return <DataPanel>
    <div className="filters-row">
      <label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar préstamo para devolución" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código o usuario" /></label>
      <label className="select-wrap"><span>Estado</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option><option value="VIGENTE">Activo</option><option value="DEVOLUCION_PARCIAL">Devolución parcial</option><option value="VENCIDO">Vencido</option></select></label>
    </div>
    <div className="metric-strip"><strong>{items.length} préstamos disponibles</strong><span>Selecciona el préstamo que deseas devolver.</span></div>
    <div className="table-scroll"><table><thead><tr><th>Código</th><th>Usuario</th><th>Implementos</th><th>Fecha préstamo</th><th>Fecha límite</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.codigo}</td><td className="strong-cell">{fullName(item.usuario)}</td><td>{item.cantidadImplementos}</td><td>{formatDate(item.fechaPrestamo)}</td><td className={item.vencido ? 'overdue' : ''}>{formatDate(item.fechaLimiteDevolucion)}</td><td><Badge tone={toneFor(item.estado)}>{displayState(item.estado)}</Badge></td><td><button disabled={busy} className="table-action" onClick={() => onSelect(item.id)}>Seleccionar</button></td></tr>)}</tbody></table></div>
    {!filtered.length && <EmptyCard title="No hay préstamos pendientes" detail="La API no devolvió préstamos disponibles para registrar una devolución." />}
    <div className="wizard-actions"><button className="secondary-button" onClick={onCancel}>Volver a devoluciones</button></div>
  </DataPanel>
}

function Returns({ items, search, setSearch, filter, setFilter, onNew }: { items: ReturnSummary[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; onNew: () => void }) { const filtered = items.filter((item) => (!filter || item.alcance === filter) && `${item.codigo} ${item.prestamo.codigo} ${fullName(item.usuario)}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))); return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar devolución" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por devolución, préstamo o usuario" /></label><label className="select-wrap"><span>Alcance</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option><option value="PARCIAL">Parcial</option><option value="COMPLETA">Completa</option></select></label><button className="primary-button nowrap" onClick={onNew}>Registrar devolución</button></div><div className="metric-strip"><strong>{items.length} devoluciones encontradas</strong><span>Las fechas y condiciones provienen de la API.</span></div><div className="table-scroll"><table><thead><tr><th>Código</th><th>Préstamo</th><th>Usuario</th><th>Fecha</th><th>Alcance</th><th>Novedad</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.codigo}</td><td>{item.prestamo.codigo}</td><td className="strong-cell">{fullName(item.usuario)}</td><td>{formatDate(item.fechaDevolucion)}</td><td><Badge tone={toneFor(item.alcance)}>{displayState(item.alcance)}</Badge></td><td>{item.conNovedad ? 'Sí' : 'No'}</td></tr>)}</tbody></table></div>{!filtered.length && <EmptyCard title="No hay devoluciones" detail="La API no devolvió registros para estos filtros." />}</DataPanel> }

function Incidents({ items, search, setSearch, filter, setFilter, role, onNew, onResolve }: { items: Incident[]; search: string; setSearch: (value: string) => void; filter: string; setFilter: (value: string) => void; role: Role; onNew: () => void; onResolve: (id: string) => void }) { const filtered = items.filter((item) => (!filter || item.tipo === filter) && `${item.codigo} ${item.implemento.codigoInventario} ${fullName(item.usuario)}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))); return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar incidencia" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por código, implemento o usuario" /></label><label className="select-wrap"><span>Tipo</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">Todos</option>{['DANO', 'PERDIDA', 'ROBO', 'OTRA'].map((type) => <option key={type} value={type}>{displayState(type)}</option>)}</select></label>{role === 'PRESTADOR' && <button className="primary-button nowrap" onClick={onNew}>Registrar incidencia</button>}</div><div className="metric-strip"><strong>{items.length} incidencias encontradas</strong><span>Estado e historial provienen de la API.</span></div><div className="table-scroll"><table><thead><tr><th>Código</th><th>Fecha</th><th>Implemento</th><th>Usuario</th><th>Tipo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.codigo}</td><td>{formatDate(item.fechaRegistro)}</td><td className="strong-cell">{item.implemento.codigoInventario}</td><td>{fullName(item.usuario)}</td><td><Badge tone={toneFor(item.tipo)}>{displayState(item.tipo)}</Badge></td><td><Badge tone={toneFor(item.estado)}>{displayState(item.estado)}</Badge></td><td>{role === 'ADMIN' && item.estado === 'REGISTRADA' && <button className="table-action" onClick={() => onResolve(item.id)}>Resolver</button>}</td></tr>)}</tbody></table></div>{!filtered.length && <EmptyCard title="No hay incidencias" detail="La API no devolvió registros para estos filtros." />}</DataPanel> }

function IncidentForm({ loans, busy, error, onCancel, onSubmit }: { loans: LoanSummary[]; busy: boolean; error: string; onCancel: () => void; onSubmit: (loanId: string, body: CreateIncidentRequest) => Promise<void> }) { const [loanId, setLoanId] = useState(loans[0]?.id ?? ''); const [implementoId, setImplementoId] = useState(''); const [tipo, setTipo] = useState<'PERDIDA' | 'ROBO'>('PERDIDA'); const [descripcion, setDescripcion] = useState(''); return <DataPanel className="form-panel"><form className="entity-form" onSubmit={(event) => { event.preventDefault(); void onSubmit(loanId, { implementoId, tipo, descripcion }) }}><h2>Datos de la incidencia</h2><p>Solo se registran pérdidas o robos independientes de una devolución.</p><label className="form-field">Préstamo<select required value={loanId} onChange={(event) => setLoanId(event.target.value)}><option value="">Selecciona un préstamo</option>{loans.filter((loan) => loan.estado !== 'FINALIZADO').map((loan) => <option key={loan.id} value={loan.id}>{loan.codigo} · {fullName(loan.usuario)}</option>)}</select></label><label className="form-field">ID del implemento pendiente<input required value={implementoId} onChange={(event) => setImplementoId(event.target.value)} placeholder="UUID de la unidad" /></label><label className="form-field">Tipo<select value={tipo} onChange={(event) => setTipo(event.target.value as 'PERDIDA' | 'ROBO')}><option value="PERDIDA">Pérdida</option><option value="ROBO">Robo</option></select></label><label className="form-field">Descripción<textarea required value={descripcion} onChange={(event) => setDescripcion(event.target.value)} rows={3} /></label>{error && <p className="field-error" role="alert">{error}</p>}<div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button disabled={busy} className="primary-button">{busy ? 'Registrando…' : 'Registrar incidencia'}</button></div></form></DataPanel> }
