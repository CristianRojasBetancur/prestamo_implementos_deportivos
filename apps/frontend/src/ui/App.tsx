import { useMemo, useState } from 'react'
import type React from 'react'

type Role = 'Administrador' | 'Prestador'
type Page = 'catalog' | 'login' | 'home' | 'inventory' | 'providers' | 'new-item' | 'edit-item' | 'new-provider' | 'edit-provider' | 'loans' | 'loan-detail' | 'loan-1' | 'loan-2' | 'loan-3' | 'returns' | 'return-form' | 'incidents' | 'new-incident'
type Equipment = { code: string; name: string; category: string; state: string; available: boolean; description?: string }
type Provider = { name: string; id: string; email: string; state: 'Activo' | 'Inactivo' }

const initialEquipment: Equipment[] = [
  { code: 'BAL-001', name: 'Balón de fútbol', category: 'Balones', state: 'Disponible', available: true },
  { code: 'BAL-003', name: 'Balón de baloncesto', category: 'Balones', state: 'Disponible', available: true },
  { code: 'RAQ-007', name: 'Raqueta de tenis', category: 'Raquetas', state: 'Disponible', available: true },
  { code: 'PET-014', name: 'Petos deportivos', category: 'Entrenamiento', state: 'Disponible', available: true },
  { code: 'CON-021', name: 'Conos', category: 'Entrenamiento', state: 'Disponible', available: true },
  { code: 'ACC-012', name: 'Cuerda para saltar', category: 'Accesorios', state: 'No disponible', available: false },
]

const initialProviders: Provider[] = [
  { name: 'María Rodríguez', id: '1012345678', email: 'maria.rodriguez@institucion.edu.co', state: 'Activo' },
  { name: 'Andrés Torres', id: '1023456789', email: 'andres.torres@institucion.edu.co', state: 'Activo' },
  { name: 'Juliana López', id: '1034567890', email: 'juliana.lopez@institucion.edu.co', state: 'Inactivo' },
  { name: 'Felipe Gómez', id: '1045678901', email: 'felipe.gomez@institucion.edu.co', state: 'Activo' },
]

const initialLoans = [
  { id: 'PRE-0148', user: 'Laura Gómez', items: '2 implementos', start: '31 ago, 18:00', end: '1 sep, 18:00', state: 'Activo' },
  { id: 'PRE-0146', user: 'Carlos Pérez', items: '1 implemento', start: '31 ago, 14:10', end: '1 sep, 14:10', state: 'Activo' },
  { id: 'PRE-0142', user: 'Mariana Ruiz', items: '1 implemento', start: '29 ago, 09:00', end: '30 ago, 09:00', state: 'Vencido' },
  { id: 'PRE-0136', user: 'Ana Rodríguez', items: '3 implementos', start: '27 ago, 16:30', end: '28 ago, 16:30', state: 'Finalizado' },
]

const initialReturns = [
  { date: '31 ago, 19:10', loan: 'PRE-0148', user: 'Laura Gómez', item: 'BAL-001', condition: 'Buen estado', scope: 'Parcial' },
  { date: '31 ago, 17:35', loan: 'PRE-0146', user: 'Carlos Pérez', item: 'RAQ-003', condition: 'Buen estado', scope: 'Completa' },
  { date: '30 ago, 16:20', loan: 'PRE-0139', user: 'Ana Rodríguez', item: '2 implementos', condition: 'Buen estado', scope: 'Completa' },
  { date: '30 ago, 11:05', loan: 'PRE-0137', user: 'Mariana Ruiz', item: 'CON-011', condition: 'Con novedad', scope: 'Parcial' },
]

const initialIncidents = [
  { date: '30 ago, 11:05', item: 'CON-011', user: 'Mariana Ruiz', type: 'Daño', state: 'Registrada' },
  { date: '29 ago, 18:42', item: 'BAL-008', user: 'Carlos Pérez', type: 'Pérdida', state: 'Resuelta' },
  { date: '28 ago, 10:15', item: 'PET-002', user: 'Ana Rodríguez', type: 'Robo', state: 'Registrada' },
  { date: '27 ago, 15:30', item: 'RAQ-002', user: 'Andrés López', type: 'Otra', state: 'Resuelta' },
]

const adminNav: { label: string; page: Page; icon: string }[] = [
  { label: 'Inicio', page: 'home', icon: '⌂' }, { label: 'Prestadores', page: 'providers', icon: '♙' }, { label: 'Inventario', page: 'inventory', icon: '▦' },
]
const providerNav: { label: string; page: Page; icon: string }[] = [
  { label: 'Inicio', page: 'home', icon: '⌂' }, { label: 'Préstamos', page: 'loans', icon: '↗' }, { label: 'Devoluciones', page: 'returns', icon: '↙' }, { label: 'Incidencias', page: 'incidents', icon: '!' },
]

const pageTitles: Partial<Record<Page, { title: string; subtitle: string }>> = {
  home: { title: 'Inicio', subtitle: 'Accede rápidamente a las operaciones principales del servicio.' },
  inventory: { title: 'Inventario', subtitle: 'Consulta, busca y administra las unidades físicas del inventario.' },
  providers: { title: 'Prestadores', subtitle: 'Consulta, busca y administra el personal autorizado.' },
  loans: { title: 'Préstamos', subtitle: 'Consulta, filtra y revisa el estado de los préstamos registrados.' },
  returns: { title: 'Devoluciones', subtitle: 'Consulta y filtra las devoluciones registradas.' },
  incidents: { title: 'Incidencias', subtitle: 'Consulta las novedades registradas sobre implementos prestados.' },
  'new-item': { title: 'Nuevo implemento', subtitle: 'Registra una nueva unidad física en el inventario.' },
  'edit-item': { title: 'Editar implemento', subtitle: 'Actualiza la información de la unidad física.' },
  'new-provider': { title: 'Nuevo prestador', subtitle: 'Registra un nuevo prestador autorizado.' },
  'edit-provider': { title: 'Editar prestador', subtitle: 'Actualiza los datos del prestador autorizado.' },
  'loan-1': { title: 'Registrar préstamo', subtitle: 'Paso 1 de 3 · Identifica y valida al usuario solicitante.' },
  'loan-2': { title: 'Registrar préstamo', subtitle: 'Paso 2 de 3 · Selecciona los implementos disponibles.' },
  'loan-3': { title: 'Registrar préstamo', subtitle: 'Paso 3 de 3 · Revisa la información antes de confirmar.' },
  'loan-detail': { title: 'Detalle del préstamo', subtitle: 'Consulta los implementos y el estado de esta entrega.' },
  'return-form': { title: 'Registrar devolución', subtitle: 'Registra la condición de los implementos y deriva una incidencia cuando sea necesario.' },
  'new-incident': { title: 'Registrar incidencia', subtitle: 'Registra una novedad asociada a un implemento prestado.' },
}

export default function App() {
  const [page, setPage] = useState<Page>('catalog')
  const [role, setRole] = useState<Role>('Prestador')
  const [menuOpen, setMenuOpen] = useState(false)
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Todas las categorías')
  const [statusFilter, setStatusFilter] = useState('Todos')
  const [equipment, setEquipment] = useState(initialEquipment)
  const [providers, setProviders] = useState(initialProviders)
  const [loans, setLoans] = useState(initialLoans)
  const [returns, setReturns] = useState(initialReturns)
  const [incidents, setIncidents] = useState(initialIncidents)
  const [selectedLoan, setSelectedLoan] = useState('PRE-0148')
  const [selectedItems, setSelectedItems] = useState<string[]>(['BAL-001', 'PET-014'])
  const [returnConditions, setReturnConditions] = useState<Record<string, string>>({ 'BAL-001': 'Buen estado', 'PET-014': 'Buen estado' })
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [form, setForm] = useState({ code: '', name: '', category: 'Balones', state: 'Disponible', description: '', id: '', email: '' })
  const title = pageTitles[page]

  const filteredCatalog = useMemo(() => equipment.filter((item) =>
    (category === 'Todas las categorías' || item.category === category) &&
    `${item.name} ${item.category}`.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es'))), [equipment, category, search])

  function go(next: Page) { setPage(next); setNotice(''); setSearch(''); setCategory('Todas las categorías'); setStatusFilter('Todos'); setMenuOpen(false) }
  function toast(message: string) { setNotice(message) }
  function startForm(type: 'item' | 'provider', existing?: Equipment | Provider) {
    setForm(type === 'item' ? { code: (existing as Equipment | undefined)?.code ?? '', name: (existing as Equipment | undefined)?.name ?? '', category: (existing as Equipment | undefined)?.category ?? 'Balones', state: (existing as Equipment | undefined)?.state ?? 'Disponible', description: (existing as Equipment | undefined)?.description ?? '', id: '', email: '' } : { code: '', name: (existing as Provider | undefined)?.name ?? '', category: '', state: '', description: '', id: (existing as Provider | undefined)?.id ?? '', email: (existing as Provider | undefined)?.email ?? '' })
    go(type === 'item' ? existing ? 'edit-item' : 'new-item' : existing ? 'edit-provider' : 'new-provider')
  }
  function saveItem(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const updated: Equipment = { code: form.code, name: form.name, category: form.category, state: form.state, available: form.state === 'Disponible', description: form.description }
    setEquipment((list) => page === 'edit-item' ? list.map((item) => item.code === form.code ? updated : item) : [...list, updated])
    toast(page === 'edit-item' ? 'Implemento actualizado en la vista de demostración.' : 'Implemento agregado a la vista de demostración.')
    go('inventory')
  }
  function saveProvider(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const updated: Provider = { name: form.name, id: form.id, email: form.email, state: 'Activo' }
    setProviders((list) => page === 'edit-provider' ? list.map((provider) => provider.id === form.id ? updated : provider) : [...list, updated])
    toast(page === 'edit-provider' ? 'Prestador actualizado en la vista de demostración.' : 'Prestador agregado a la vista de demostración.')
    go('providers')
  }
  function login(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!loginEmail || !loginPassword) { toast('Este campo es obligatorio.'); return }
    go('home')
    toast('Vista de demostración activa. La cuenta no se validó contra un servicio.')
  }
  function submitLoan() {
    setLoans((list) => [{ id: 'PRE-0149', user: 'Laura Gómez', items: `${selectedItems.length} implementos`, start: '31 ago, 18:00', end: '1 sep, 18:00', state: 'Activo' }, ...list])
    go('loans')
    toast('Préstamo agregado a la vista de demostración.')
  }
  function submitReturn() {
    const hasNovelty = Object.values(returnConditions).some((condition) => condition !== 'Buen estado')
    if (hasNovelty) { go('new-incident'); return }
    setReturns((list) => [{ date: '31 ago, 19:10', loan: selectedLoan, user: 'Laura Gómez', item: 'BAL-001', condition: 'Buen estado', scope: 'Parcial' }, ...list])
    toast('Devolución agregada a la vista de demostración.')
    go('returns')
  }
  function deleteItem(code: string) { setEquipment((list) => list.filter((item) => item.code !== code)); toast('Implemento retirado de la vista de demostración.') }
  function toggleSelected(code: string) { setSelectedItems((items) => items.includes(code) ? items.filter((item) => item !== code) : [...items, code]) }

  if (page === 'catalog') return <PublicCatalog items={filteredCatalog} search={search} setSearch={setSearch} category={category} setCategory={setCategory} onLogin={() => go('login')} />
  if (page === 'login') return <Login role={role} setRole={setRole} email={loginEmail} setEmail={setLoginEmail} password={loginPassword} setPassword={setLoginPassword} onSubmit={login} onBack={() => go('catalog')} notice={notice} />

  const nav = role === 'Administrador' ? adminNav : providerNav
  return <div className="prototype-app">
    <aside className={`app-sidebar ${menuOpen ? 'open' : ''}`}>
      <div className="sidebar-brand"><strong>Préstamos deportivos</strong><span>{role}</span></div>
      <nav aria-label="Navegación principal" className="app-nav">{nav.map((item) => <button key={item.page} className={page === item.page || (item.page === 'inventory' && ['new-item', 'edit-item'].includes(page)) || (item.page === 'providers' && ['new-provider', 'edit-provider'].includes(page)) || (item.page === 'loans' && ['loan-1', 'loan-2', 'loan-3', 'loan-detail'].includes(page)) || (item.page === 'returns' && page === 'return-form') || (item.page === 'incidents' && page === 'new-incident') ? 'selected' : ''} onClick={() => go(item.page)}><i>{item.icon}</i>{item.label}</button>)}</nav>
      <div className="sidebar-session"><strong>Usuario autorizado</strong><span>{role}</span></div>
    </aside>
    {menuOpen && <button className="sidebar-scrim" aria-label="Cerrar navegación" onClick={() => setMenuOpen(false)} />}
    <div className="prototype-main">
      <header className="app-topbar"><button className="menu-toggle" aria-label="Abrir navegación" onClick={() => setMenuOpen(!menuOpen)}>☰</button><span>Sistema de Préstamo de Implementos Deportivos</span><div className="session"><i /> <span><strong>Usuario autorizado</strong><small>Sesión activa</small></span><button className="top-role" onClick={() => go('catalog')} title="Volver al catálogo público">Salir</button></div></header>
      <main className="app-content">
        {title && <PageHeading title={title.title} subtitle={title.subtitle} />}
        {page === 'home' && <Home role={role} go={go} />}
        {page === 'inventory' && <Inventory items={equipment} search={search} setSearch={setSearch} category={category} setCategory={setCategory} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onNew={() => startForm('item')} onEdit={(item) => startForm('item', item)} onDelete={deleteItem} />}
        {page === 'providers' && <Providers items={providers} search={search} setSearch={setSearch} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onNew={() => startForm('provider')} onEdit={(provider) => startForm('provider', provider)} onToggle={(id) => setProviders((list) => list.map((item) => item.id === id ? { ...item, state: item.state === 'Activo' ? 'Inactivo' : 'Activo' } : item))} />}
        {(page === 'new-item' || page === 'edit-item') && <ItemForm form={form} setForm={setForm} onSubmit={saveItem} onCancel={() => go('inventory')} />}
        {(page === 'new-provider' || page === 'edit-provider') && <ProviderForm form={form} setForm={setForm} onSubmit={saveProvider} onCancel={() => go('providers')} />}
        {page === 'loans' && <Loans items={loans} search={search} setSearch={setSearch} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onDetails={(id) => { setSelectedLoan(id); go('loan-detail') }} />}
        {page === 'loan-detail' && <LoanDetail loan={loans.find((item) => item.id === selectedLoan) ?? loans[0]} onReturn={() => go('return-form')} onBack={() => go('loans')} />}
        {page.startsWith('loan-') && page !== 'loan-detail' && <LoanWizard step={Number(page.at(-1))} selectedItems={selectedItems} toggleSelected={toggleSelected} onStep={(step) => go(`loan-${step}` as Page)} onFinish={submitLoan} />}
        {page === 'returns' && <Returns items={returns} search={search} setSearch={setSearch} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onNew={() => go('return-form')} />}
        {page === 'return-form' && <ReturnForm selectedLoan={selectedLoan} conditions={returnConditions} setConditions={setReturnConditions} onBack={() => go('returns')} onSubmit={submitReturn} />}
        {page === 'incidents' && <Incidents items={incidents} search={search} setSearch={setSearch} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onNew={() => go('new-incident')} />}
        {page === 'new-incident' && <IncidentForm onCancel={() => go('incidents')} onSubmit={(type) => { setIncidents((list) => [{ date: '31 ago, 19:10', item: 'PET-014', user: 'Laura Gómez', type, state: 'Registrada' }, ...list]); go('incidents'); toast('Incidencia agregada a la vista de demostración.') }} />}
        {notice && <div className="toast" role="status"><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Cerrar mensaje">×</button></div>}
      </main>
    </div>
  </div>
}

function PublicCatalog({ items, search, setSearch, category, setCategory, onLogin }: { items: Equipment[]; search: string; setSearch: (value: string) => void; category: string; setCategory: (value: string) => void; onLogin: () => void }) {
  return <main className="public-page">
    <header className="public-header"><div className="institution-logo">GD</div><span><strong>Gestión Deportiva</strong><small>Institución académica</small></span><button onClick={onLogin}>Acceso institucional</button></header>
    <section className="public-content"><h1>Implementos deportivos</h1><p className="page-subtitle">Consulta la disponibilidad actual antes de dirigirte al punto de préstamo.</p>
      <div className="catalog-filters"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar implementos" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar implementos" /></label><label className="select-wrap"><span>Categoría</span><select aria-label="Categoría" value={category} onChange={(event) => setCategory(event.target.value)}><option>Todas las categorías</option>{[...new Set(initialEquipment.map((item) => item.category))].map((item) => <option key={item}>{item}</option>)}</select></label></div>
      <p className="record-count">{items.length} implementos</p>
      {items.length ? <div className="equipment-grid">{items.map((item) => <article className="equipment-card" key={item.code}><h2>{item.name}</h2><p>{item.category}</p><Badge tone={item.available ? 'success' : 'neutral'}>{item.available ? 'Disponible' : 'No disponible'}</Badge></article>)}</div> : <EmptyCard title="No encontramos resultados" detail="Prueba con otro término de búsqueda o cambia la categoría." />}
    </section>
  </main>
}

function Login({ role, setRole, email, setEmail, password, setPassword, onSubmit, onBack, notice }: { role: Role; setRole: (role: Role) => void; email: string; setEmail: (value: string) => void; password: string; setPassword: (value: string) => void; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; onBack: () => void; notice: string }) {
  return <main className="login-page"><button className="back-link" onClick={onBack}>← Volver al catálogo</button><form className="login-card" onSubmit={onSubmit}>
    <div className="institution-logo">GD</div><div className="institution-name"><strong>Gestión Deportiva</strong><small>Institución académica</small></div>
    <h1>Iniciar sesión</h1><p className="login-intro">Ingresa tus credenciales para acceder a las funciones correspondientes a tu rol.</p>
    <div className="role-selector" aria-label="Rol de acceso"><button type="button" className={role === 'Prestador' ? 'active' : ''} onClick={() => setRole('Prestador')}>Prestador</button><button type="button" className={role === 'Administrador' ? 'active' : ''} onClick={() => setRole('Administrador')}>Administrador</button></div>
    <label className="form-field">Correo o identificación<input type="text" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="Ingresa tu correo o identificación" /></label>
    <label className="form-field">Contraseña<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Ingresa tu contraseña" /></label>
    {notice && <p className="field-error">{notice}</p>}
    <button className="primary-button full-width">Iniciar sesión</button>
    <p className="login-foot">Acceso exclusivo para personal autorizado.</p>
  </form></main>
}

function PageHeading({ title, subtitle }: { title: string; subtitle: string }) { return <div className="page-heading"><h1>{title}</h1><p>{subtitle}</p></div> }
function Badge({ tone, children }: { tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral'; children: React.ReactNode }) { return <span className={`badge ${tone}`}>{children}</span> }
function EmptyCard({ title, detail }: { title: string; detail: string }) { return <div className="empty-card"><span>−</span><strong>{title}</strong><p>{detail}</p></div> }
function DataPanel({ children, className = '' }: { children: React.ReactNode; className?: string }) { return <section className={`data-panel ${className}`}>{children}</section> }

function Home({ role, go }: { role: Role; go: (page: Page) => void }) {
  return role === 'Administrador' ? <DataPanel className="home-panel"><h2>Resumen administrativo</h2><div className="summary-grid"><SummaryCard label="Prestadores activos" text="12 habilitados" /><SummaryCard label="Implementos disponibles" text="86 unidades" /><SummaryCard label="No disponibles" text="14 unidades" /></div><h2 className="section-label">Acciones principales</h2><div className="action-grid"><ActionCard title="Gestionar prestadores" text="Crea, consulta, actualiza y desactiva prestadores autorizados." button="Gestionar prestadores" onClick={() => go('providers')} primary /><ActionCard title="Gestionar inventario" text="Consulta, registra y administra las unidades físicas del inventario." button="Gestionar inventario" onClick={() => go('inventory')} /></div><div className="info-strip">Prioriza la gestión de prestadores y el control operativo del inventario.</div></DataPanel> : <DataPanel className="home-panel"><h2>Resumen operativo</h2><div className="summary-grid two"><SummaryCard label="Préstamos activos" text="18 en curso" /><SummaryCard label="Préstamos vencidos" text="3 requieren atención" /></div><h2 className="section-label">Acciones rápidas</h2><p className="panel-subtitle">Accede a las operaciones más frecuentes del punto de préstamo.</p><div className="action-grid three"><ActionCard title="Registrar préstamo" text="Identifica al usuario, selecciona implementos disponibles y confirma la entrega." button="Registrar préstamo" onClick={() => go('loan-1')} primary /><ActionCard title="Registrar devolución" text="Localiza un préstamo activo y registra los implementos que regresan." button="Registrar devolución" onClick={() => go('return-form')} /><ActionCard title="Consultar préstamos" text="Revisa préstamos activos, vencidos y finalizados desde un solo lugar." button="Ver préstamos" onClick={() => go('loans')} /></div><div className="info-strip">Antes de entregar implementos, verifica que el usuario esté habilitado y no tenga préstamos vencidos.</div></DataPanel>
}
function SummaryCard({ label, text }: { label: string; text: string }) { return <article className="summary-card"><strong>{label}</strong><span>{text}</span></article> }
function ActionCard({ title, text, button, onClick, primary = false }: { title: string; text: string; button: string; onClick: () => void; primary?: boolean }) { return <article className="action-card"><strong>{title}</strong><p>{text}</p><button className={primary ? 'primary-button' : 'secondary-button'} onClick={onClick}>{button}</button></article> }

function Inventory({ items, search, setSearch, category, setCategory, statusFilter, setStatusFilter, onNew, onEdit, onDelete }: { items: Equipment[]; search: string; setSearch: (v: string) => void; category: string; setCategory: (v: string) => void; statusFilter: string; setStatusFilter: (v: string) => void; onNew: () => void; onEdit: (item: Equipment) => void; onDelete: (code: string) => void }) {
  const filtered = items.filter((item) => (category === 'Todas' || category === 'Todas las categorías' || item.category === category) && (statusFilter === 'Todos' || item.state === statusFilter) && `${item.code} ${item.name}`.toLowerCase().includes(search.toLowerCase()))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar implemento" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por código o nombre" /></label><label className="select-wrap"><span>Categoría</span><select value={category} onChange={(e) => setCategory(e.target.value)}><option>Todas</option>{[...new Set(items.map((item) => item.category))].map((v) => <option key={v}>{v}</option>)}</select></label><label className="select-wrap"><span>Estado</span><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>Todos</option><option>Disponible</option><option>Prestado</option><option>En mantenimiento</option><option>Dado de baja</option></select></label><button className="primary-button nowrap" onClick={onNew}>＋ Nuevo implemento</button></div>
    <div className="metric-strip"><strong>{items.length === initialEquipment.length ? '100 unidades registradas' : `${items.length} unidades registradas`}</strong><span>86 disponibles · 14 no disponibles</span></div>
    <div className="table-scroll"><table><thead><tr><th>Código</th><th>Nombre</th><th>Categoría</th><th>Estado</th><th>Disponibilidad</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.code}><td>{item.code}</td><td className="strong-cell">{item.name}</td><td>{item.category}</td><td><Badge tone={stateTone(item.state)}>{item.state}</Badge></td><td><Badge tone={item.available ? 'success' : 'neutral'}>{item.available ? 'Disponible' : 'No disponible'}</Badge></td><td className="actions-cell"><button className="table-action" onClick={() => onEdit(item)}>Editar</button>{item.state !== 'Prestado' && <button className="danger-button compact" onClick={() => onDelete(item.code)}>Retirar</button>}</td></tr>)}</tbody></table></div><p className="table-note">Cada fila representa una unidad física individual del inventario.</p>
  </DataPanel>
}

function Providers({ items, search, setSearch, statusFilter, setStatusFilter, onNew, onEdit, onToggle }: { items: Provider[]; search: string; setSearch: (v: string) => void; statusFilter: string; setStatusFilter: (v: string) => void; onNew: () => void; onEdit: (item: Provider) => void; onToggle: (id: string) => void }) {
  const filtered = items.filter((item) => (statusFilter === 'Todos' || item.state === statusFilter) && `${item.name} ${item.id} ${item.email}`.toLowerCase().includes(search.toLowerCase()))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar prestador" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por nombre, identificación o correo" /></label><label className="select-wrap"><span>Estado</span><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>Todos</option><option>Activo</option><option>Inactivo</option></select></label><button className="primary-button nowrap" onClick={onNew}>＋ Nuevo prestador</button></div><div className="metric-strip"><strong>{items.length === initialProviders.length ? '15 prestadores encontrados' : `${items.length} prestadores encontrados`}</strong><span>12 activos · 3 inactivos</span></div><div className="table-scroll"><table><thead><tr><th>Nombre</th><th>Identificación</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td className="strong-cell">{item.name}</td><td>{item.id}</td><td>{item.email}</td><td><Badge tone={item.state === 'Activo' ? 'success' : 'neutral'}>{item.state}</Badge></td><td className="actions-cell"><button className="table-action" onClick={() => onEdit(item)}>Editar</button><button className="danger-button compact" onClick={() => onToggle(item.id)}>{item.state === 'Activo' ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table></div><p className="table-note">Mostrando {filtered.length} de {items.length}</p></DataPanel>
}

function ItemForm({ form, setForm, onSubmit, onCancel }: { form: { code: string; name: string; category: string; state: string; description: string }; setForm: React.Dispatch<React.SetStateAction<{ code: string; name: string; category: string; state: string; description: string; id: string; email: string }>>; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; onCancel: () => void }) {
  return <DataPanel className="form-panel"><form className="entity-form" onSubmit={onSubmit}><h2>Datos del nuevo implemento</h2><p>Registra una unidad física del inventario deportivo.</p><div className="form-grid"><label className="form-field">Código de inventario<input required value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))} placeholder="Ej. BAL-003" /></label><label className="form-field">Nombre<input required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="Nombre del implemento" /></label><label className="form-field">Categoría<select value={form.category} onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}><option>Balones</option><option>Raquetas</option><option>Entrenamiento</option><option>Accesorios</option></select></label><label className="form-field">Estado<select value={form.state} onChange={(e) => setForm((f) => ({ ...f, state: e.target.value }))}><option>Disponible</option><option>En mantenimiento</option><option>Dado de baja</option></select></label><label className="form-field full-span">Descripción<input value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} placeholder="Descripción breve del implemento" /></label></div><div className="info-strip">La disponibilidad operativa depende del estado actual del implemento.</div><div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button className="primary-button">{form.code ? 'Guardar cambios' : 'Crear implemento'}</button></div></form></DataPanel>
}
function ProviderForm({ form, setForm, onSubmit, onCancel }: { form: { name: string; id: string; email: string }; setForm: React.Dispatch<React.SetStateAction<{ code: string; name: string; category: string; state: string; description: string; id: string; email: string }>>; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; onCancel: () => void }) {
  return <DataPanel className="form-panel"><form className="entity-form provider-form" onSubmit={onSubmit}><h2>Datos del nuevo prestador</h2><p>Registra los datos necesarios para autorizar a un nuevo prestador.</p><label className="form-field">Nombre completo<input required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="Nombre y apellidos" /></label><label className="form-field">Identificación<input required value={form.id} onChange={(e) => setForm((f) => ({ ...f, id: e.target.value }))} placeholder="Número de identificación" /></label><label className="form-field">Correo institucional<input type="email" required value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} placeholder="correo@universidad.edu.co" /></label><div className="role-row"><span>Rol del sistema</span><strong>Prestador</strong></div><div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button className="primary-button">{form.id ? 'Guardar cambios' : 'Crear prestador'}</button></div></form></DataPanel>
}

function Loans({ items, search, setSearch, statusFilter, setStatusFilter, onDetails }: { items: typeof initialLoans; search: string; setSearch: (v: string) => void; statusFilter: string; setStatusFilter: (v: string) => void; onDetails: (id: string) => void }) {
  const filtered = items.filter((item) => (statusFilter === 'Todos los estados' || item.state === statusFilter) && `${item.id} ${item.user}`.toLowerCase().includes(search.toLowerCase()))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar préstamo" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por ID o usuario" /></label><label className="select-wrap"><span>Estado</span><select value={statusFilter === 'Todos' ? 'Todos los estados' : statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>Todos los estados</option><option>Activo</option><option>Vencido</option><option>Finalizado</option></select></label></div><div className="metric-strip"><strong>{items.length} préstamos encontrados</strong><span>2 activos · 1 vencido · 1 finalizado</span></div><div className="table-scroll"><table><thead><tr><th>ID</th><th>Usuario</th><th>Implementos</th><th>Fecha préstamo</th><th>Fecha límite</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={item.id}><td>{item.id}</td><td className="strong-cell">{item.user}</td><td>{item.items}</td><td>{item.start}</td><td className={item.state === 'Vencido' ? 'overdue' : ''}>{item.end}</td><td><Badge tone={item.state === 'Activo' ? 'success' : item.state === 'Vencido' ? 'danger' : 'neutral'}>{item.state}</Badge></td><td><button className="table-action" onClick={() => onDetails(item.id)}>Ver detalle</button></td></tr>)}</tbody></table></div><p className="table-note">Los préstamos vencidos requieren atención antes de registrar nuevas entregas.</p></DataPanel>
}

function Returns({ items, search, setSearch, statusFilter, setStatusFilter, onNew }: { items: typeof initialReturns; search: string; setSearch: (v: string) => void; statusFilter: string; setStatusFilter: (v: string) => void; onNew: () => void }) {
  const filtered = items.filter((item) => (statusFilter === 'Todos' || item.scope === statusFilter) && `${item.loan} ${item.user}`.toLowerCase().includes(search.toLowerCase()))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar devolución" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por préstamo o usuario" /></label><label className="select-wrap"><span>Alcance</span><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>Todos</option><option>Parcial</option><option>Completa</option></select></label><button className="primary-button nowrap" onClick={onNew}>Registrar devolución</button></div><div className="metric-strip"><strong>{items.length} devoluciones encontradas · última DEV-0062</strong><span>2 parciales · 2 completas</span></div><div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Préstamo</th><th>Usuario</th><th>Implementos</th><th>Condición</th><th>Alcance</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={`${item.loan}${item.date}`}><td>{item.date}</td><td>{item.loan}</td><td className="strong-cell">{item.user}</td><td>{item.item}</td><td><Badge tone={item.condition === 'Buen estado' ? 'success' : 'danger'}>{item.condition}</Badge></td><td><Badge tone={item.scope === 'Parcial' ? 'info' : 'neutral'}>{item.scope}</Badge></td><td><button className="table-action">Ver detalle</button></td></tr>)}</tbody></table></div><p className="table-note">Una devolución parcial mantiene el préstamo activo mientras existan implementos pendientes.</p></DataPanel>
}

function Incidents({ items, search, setSearch, statusFilter, setStatusFilter, onNew }: { items: typeof initialIncidents; search: string; setSearch: (v: string) => void; statusFilter: string; setStatusFilter: (v: string) => void; onNew: () => void }) {
  const filtered = items.filter((item) => (statusFilter === 'Todos los tipos' || item.type === statusFilter) && `${item.item} ${item.user}`.toLowerCase().includes(search.toLowerCase()))
  return <DataPanel><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input aria-label="Buscar incidencia" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por implemento o usuario" /></label><label className="select-wrap"><span>Tipo</span><select value={statusFilter === 'Todos' ? 'Todos los tipos' : statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option>Todos los tipos</option><option>Daño</option><option>Pérdida</option><option>Robo</option><option>Otra</option></select></label><button className="primary-button nowrap" onClick={onNew}>Registrar incidencia</button></div><div className="metric-strip"><strong>{items.length} incidencias encontradas</strong><span>2 registradas · 2 resueltas</span></div><div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Implemento</th><th>Usuario</th><th>Tipo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filtered.map((item) => <tr key={`${item.item}${item.date}`}><td>{item.date}</td><td className="strong-cell">{item.item}</td><td>{item.user}</td><td><Badge tone={item.type === 'Otra' ? 'neutral' : 'danger'}>{item.type}</Badge></td><td><Badge tone={item.state === 'Resuelta' ? 'success' : 'info'}>{item.state}</Badge></td><td><button className="table-action">Ver detalle</button></td></tr>)}</tbody></table></div><p className="table-note">Las incidencias mantienen el implemento fuera de disponibilidad hasta revisar su situación.</p></DataPanel>
}

function LoanWizard({ step, selectedItems, toggleSelected, onStep, onFinish }: { step: number; selectedItems: string[]; toggleSelected: (code: string) => void; onStep: (step: number) => void; onFinish: () => void }) {
  const units = initialEquipment.filter((item) => item.available).slice(0, 3)
  return <DataPanel className="wizard-panel"><div className="step-intro"><Badge tone="info">Paso {step} de 3</Badge><span>{step === 1 ? 'Busca al usuario y confirma que puede recibir un nuevo préstamo.' : step === 2 ? 'Selecciona únicamente unidades disponibles. El usuario puede tener máximo 4 implementos activos.' : 'Confirma la información antes de registrar la entrega.'}</span></div>
    {step === 1 && <><label className="search-input wizard-search"><span className="search-glyph">⌕</span><input placeholder="Buscar por nombre o identificación" aria-label="Buscar usuario" /></label><div className="user-status"><div><strong>Laura Gómez</strong><span>Estudiante · ID 102334455</span></div><Badge tone="success">Habilitada</Badge></div><div className="summary-grid three"><SummaryCard label="Préstamos activos" text="1 préstamo activo" /><SummaryCard label="Préstamos permitidos" text="3 disponibles" /><SummaryCard label="Préstamos vencidos" text="0 pendientes" /></div><div className="success-strip">Usuario habilitado · Puede recibir hasta 3 implementos adicionales.</div><div className="wizard-actions"><button className="secondary-button">Buscar otro usuario</button><button className="primary-button" onClick={() => onStep(2)}>Continuar con este usuario</button></div></>}
    {step === 2 && <><div className="user-status compact-status"><strong>Laura Gómez</strong><span>1 implemento activo · 0 préstamos vencidos</span><Badge tone="success">Habilitada</Badge></div><div className="filters-row"><label className="search-input"><span className="search-glyph">⌕</span><input placeholder="Buscar implemento" aria-label="Buscar implemento" /></label><label className="select-wrap"><span>Categoría</span><select><option>Todas las categorías</option></select></label></div><div className="selectable-grid">{units.map((item) => <button type="button" className={`selectable-card ${selectedItems.includes(item.code) ? 'checked' : ''}`} onClick={() => toggleSelected(item.code)} key={item.code}><strong>{item.name}</strong><span>{item.category}</span><Badge tone="success">Disponible</Badge></button>)}</div><div className="metric-strip"><strong>{selectedItems.length} seleccionados · Puedes agregar {Math.max(0, 3 - selectedItems.length)} implemento(s) más.</strong><span>{selectedItems.map((code) => initialEquipment.find((item) => item.code === code)?.name).join(' · ')}</span></div><div className="wizard-actions"><button className="secondary-button" onClick={() => onStep(1)}>Volver</button><button className="primary-button" onClick={() => onStep(3)}>Continuar con {selectedItems.length} implementos</button></div></>}
    {step === 3 && <><div className="user-status compact-status"><strong>Laura Gómez</strong><span>Estudiante · ID 102334455 · 1 implemento activo</span><Badge tone="success">Habilitada</Badge></div><div className="confirm-grid"><div className="summary-card"><strong>Implementos seleccionados</strong>{selectedItems.map((code) => <span key={code}>{initialEquipment.find((item) => item.code === code)?.name} · {code}</span>)}<small>Total al confirmar: {selectedItems.length + 1} implementos activos de 4 permitidos.</small></div><div className="summary-card"><strong>Fechas del préstamo</strong><span>Fecha de entrega<br /><b>31 ago 2026, 18:00</b></span><span>Fecha límite<br /><b>1 sep 2026, 18:00</b></span><Badge tone="warning">Duración regular: 24 horas</Badge></div></div><div className="warning-strip"><strong>Cambio de disponibilidad</strong><span>Al confirmar, los implementos seleccionados pasan a Prestado y dejarán de estar disponibles.</span></div><div className="wizard-actions"><button className="secondary-button" onClick={() => onStep(2)}>Volver</button><button className="primary-button" onClick={onFinish}>Confirmar préstamo</button></div></>}
  </DataPanel>
}

function LoanDetail({ loan, onReturn, onBack }: { loan: typeof initialLoans[number]; onReturn: () => void; onBack: () => void }) { return <DataPanel className="detail-panel"><div className="metric-strip"><strong>{loan.id}</strong><Badge tone="success">{loan.state}</Badge><span>{loan.user} · {loan.items}</span></div><div className="confirm-grid"><div className="summary-card"><strong>Implementos del préstamo</strong><span>Balón de fútbol · BAL-001 <Badge tone="success">Disponible para devolución</Badge></span><span>Petos deportivos · PET-014 <Badge tone="info">Pendiente devolución</Badge></span></div><div className="summary-card"><strong>Fechas del préstamo</strong><span>Fecha de entrega <b>{loan.start}</b></span><span>Fecha límite <b>{loan.end}</b></span></div></div><div className="summary-grid three"><SummaryCard label="Devoluciones" text="0 registradas" /><SummaryCard label="Incidencias" text="0 asociadas" /><SummaryCard label="Estado operativo" text="El préstamo sigue activo hasta devolver o resolver todos los implementos." /></div><div className="wizard-actions"><button className="secondary-button" onClick={onBack}>Volver a préstamos</button><button className="primary-button" onClick={onReturn}>Registrar devolución</button></div></DataPanel> }

function ReturnForm({ selectedLoan, conditions, setConditions, onBack, onSubmit }: { selectedLoan: string; conditions: Record<string, string>; setConditions: React.Dispatch<React.SetStateAction<Record<string, string>>>; onBack: () => void; onSubmit: () => void }) {
  const hasNovelty = Object.values(conditions).some((condition) => condition !== 'Buen estado')
  return <DataPanel className="wizard-panel"><div className="metric-strip"><strong>{selectedLoan}</strong><Badge tone="success">Activo</Badge><span>Laura Gómez · 2 implementos seleccionados</span></div>{hasNovelty && <div className="error-strip"><strong>Requiere incidencia</strong><span>Un implemento presenta una novedad. Registra la incidencia antes de resolver su devolución.</span></div>}{['BAL-001', 'PET-014'].map((code, index) => <div className={`return-item ${conditions[code] !== 'Buen estado' ? 'needs-review' : ''}`} key={code}><div><strong>{index === 0 ? 'Balón de fútbol' : 'Petos deportivos'}</strong><span>{code} · {index === 0 ? 'Balones' : 'Entrenamiento'}</span><small>{conditions[code] === 'Buen estado' ? 'Volverá a Disponible al confirmar.' : 'No volverá automáticamente a Disponible.'}</small></div><label className="select-wrap"><span>Condición</span><select value={conditions[code] ?? 'Buen estado'} onChange={(e) => setConditions((prev) => ({ ...prev, [code]: e.target.value }))}><option>Buen estado</option><option>Con novedad</option></select></label></div>)}<div className={hasNovelty ? 'error-strip' : 'info-strip'}>{hasNovelty ? 'PET-014 necesita una incidencia asociada.' : 'Los implementos en buen estado volverán a estar disponibles.'}</div><div className="wizard-actions"><button className="secondary-button" onClick={onBack}>Volver</button><button className="primary-button" onClick={onSubmit}>{hasNovelty ? 'Registrar incidencia' : 'Confirmar devolución'}</button></div></DataPanel>
}

function IncidentForm({ onCancel, onSubmit }: { onCancel: () => void; onSubmit: (type: string) => void }) { const [type, setType] = useState('Daño'); return <DataPanel className="form-panel"><form className="entity-form provider-form" onSubmit={(event) => { event.preventDefault(); onSubmit(type) }}><h2>Datos de la incidencia</h2><p>Relaciona la novedad con un préstamo y una unidad física.</p><label className="form-field">Préstamo<select><option>PRE-0148 · Laura Gómez</option></select></label><label className="form-field">Implemento<select><option>PET-014 · Petos deportivos</option></select></label><label className="form-field">Tipo<select value={type} onChange={(e) => setType(e.target.value)}><option>Daño</option><option>Pérdida</option><option>Robo</option><option>Otra</option></select></label><label className="form-field">Descripción<textarea placeholder="Describe la novedad observada" rows={3} /></label><div className="warning-strip">El implemento permanecerá fuera de disponibilidad mientras se revisa la incidencia.</div><div className="form-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button className="primary-button">Registrar incidencia</button></div></form></DataPanel> }

function stateTone(state: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' { if (state === 'Disponible' || state === 'Activo' || state === 'Buen estado' || state === 'Resuelta') return 'success'; if (state === 'En mantenimiento') return 'warning'; if (state === 'Vencido' || state === 'Con novedad' || state === 'Dado de baja') return 'danger'; if (state === 'Prestado' || state === 'Registrada') return 'info'; return 'neutral' }
