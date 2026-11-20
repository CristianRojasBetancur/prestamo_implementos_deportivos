import { query, request } from './client'
import type {
  AvailabilityItem, Category, CreateIncidentRequest, CreateLoanRequest, CreateReturnRequest, Incident, InventoryCreateRequest,
  InventoryPage, InventoryStateRequest, InventoryUnit, InventoryUpdateRequest, Loan, LoanSummary, LoginRequest, LoginResponse,
  PageResponse, Provider, ProviderCreateRequest, ProviderUpdateRequest, ReturnDetail, ReturnSummary, SessionUser, UserSituation,
  UserSummary, CreateUserRequest,
} from './types'

const pageParams = (params: Record<string, string | number | undefined>) => ({ pagina: 0, tamano: 20, ...params })

export const authApi = {
  login: (body: LoginRequest) => request<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request<SessionUser>('/auth/me'),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
}

export const catalogApi = {
  availability: (params: { q?: string; categoriaId?: string; pagina?: number; tamano?: number } = {}) => request<PageResponse<AvailabilityItem>>(`/availability${query(pageParams(params))}`),
  categories: () => request<Category[]>('/categorias'),
}

export const providersApi = {
  list: (params: { q?: string; estado?: string; pagina?: number; tamano?: number } = {}) => request<PageResponse<Provider>>(`/providers${query(pageParams(params))}`),
  create: (body: ProviderCreateRequest) => request<Provider>('/providers', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: ProviderUpdateRequest) => request<Provider>(`/providers/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  changeState: (id: string, estado: 'ACTIVO' | 'INACTIVO') => request<Provider>(`/providers/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }),
}

export const inventoryApi = {
  list: (params: { q?: string; categoriaId?: string; estado?: string; pagina?: number; tamano?: number } = {}) => request<InventoryPage>(`/inventory/units${query(pageParams(params))}`),
  available: (params: { q?: string; categoriaId?: string; pagina?: number; tamano?: number } = {}) => request<InventoryPage>(`/implementos/prestables${query(pageParams(params))}`),
  create: (body: InventoryCreateRequest) => request<InventoryUnit>('/inventory/units', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: InventoryUpdateRequest) => request<InventoryUnit>(`/inventory/units/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  changeState: (id: string, body: InventoryStateRequest) => request<InventoryUnit>(`/inventory/units/${id}/state`, { method: 'PATCH', body: JSON.stringify(body) }),
}

export const usersApi = {
  list: (q?: string) => request<PageResponse<UserSummary>>(`/usuarios${query(pageParams({ q }))}`),
  situation: (id: string) => request<UserSituation>(`/usuarios/${id}/situacion`),
  create: (body: CreateUserRequest) => request<UserSummary>('/usuarios', { method: 'POST', body: JSON.stringify(body) }),
}

export const loansApi = {
  list: (params: { q?: string; estado?: string; pagina?: number; tamano?: number } = {}) => request<PageResponse<LoanSummary>>(`/prestamos${query(pageParams(params))}`),
  get: (id: string) => request<Loan>(`/prestamos/${id}`),
  create: (body: CreateLoanRequest) => request<Loan>('/prestamos', { method: 'POST', body: JSON.stringify(body) }),
}

export const returnsApi = {
  list: (params: { q?: string; alcance?: string; pagina?: number; tamano?: number } = {}) => request<PageResponse<ReturnSummary>>(`/devoluciones${query(pageParams(params))}`),
  get: (id: string) => request<ReturnDetail>(`/devoluciones/${id}`),
  create: (loanId: string, body: CreateReturnRequest) => request<ReturnDetail>(`/prestamos/${loanId}/devoluciones`, { method: 'POST', body: JSON.stringify(body) }),
}

export const incidentsApi = {
  list: (params: { q?: string; tipo?: string; estado?: string; pagina?: number; tamano?: number } = {}) => request<PageResponse<Incident>>(`/incidencias${query(pageParams(params))}`),
  get: (id: string) => request<Incident>(`/incidencias/${id}`),
  create: (loanId: string, body: CreateIncidentRequest) => request<Incident>(`/prestamos/${loanId}/incidencias`, { method: 'POST', body: JSON.stringify(body) }),
  resolve: (id: string) => request<Incident>(`/incidencias/${id}/resolucion`, { method: 'PATCH' }),
}
