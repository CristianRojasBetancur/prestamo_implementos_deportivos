export type Role = 'ADMIN' | 'PRESTADOR'

export type PageResponse<T> = {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export type Category = { id: string; nombre: string; descripcion?: string | null; activa?: boolean }
export type CategoryReference = { id: string; nombre: string }

export type AvailabilityItem = {
  nombre: string
  categoria: CategoryReference
  unidadesDisponibles: number
  unidadesTotales: number
  disponible: boolean
}

export type SessionUser = {
  id: string
  nombres: string
  apellidos: string
  correo?: string | null
  rol: Role
}

export type LoginRequest = { identificador: string; contrasena: string }
export type LoginResponse = { tokenAcceso: string; tipoToken: 'Bearer'; expiraEn: string; usuario: SessionUser }

export type Provider = {
  id: string
  nombres: string
  apellidos: string
  identificacion: string
  correo: string
  rol: 'PRESTADOR'
  estado: 'ACTIVO' | 'INACTIVO'
  creadoEn: string
  actualizadoEn?: string
}
export type ProviderCreateRequest = { nombres: string; apellidos: string; identificacion: string; correo: string; contrasenaInicial: string }
export type ProviderUpdateRequest = Omit<ProviderCreateRequest, 'contrasenaInicial'>

export type InventoryState = 'DISPONIBLE' | 'PRESTADO' | 'EN_MANTENIMIENTO' | 'DADO_DE_BAJA'
export type InventoryUnit = {
  id: string
  codigoInventario: string
  nombre: string
  descripcion?: string | null
  categoria: CategoryReference
  estado: InventoryState
  prestable: boolean
  fechaRegistro: string
}
export type InventoryPage = PageResponse<InventoryUnit> & { resumen?: Record<string, number> }
export type InventoryCreateRequest = { codigoInventario: string; nombre: string; categoriaId: string; descripcion?: string; estado?: Extract<InventoryState, 'DISPONIBLE' | 'EN_MANTENIMIENTO'> }
export type InventoryUpdateRequest = Omit<InventoryCreateRequest, 'estado'>
export type InventoryStateRequest = { estado: InventoryState; motivo: string }

export type UserSummary = { id: string; nombres: string; apellidos: string; identificacion: string; activo: boolean }
export type CreateUserRequest = { identificacion: string; nombres: string; apellidos: string }
export type UserSituation = {
  usuario: UserSummary
  habilitado: boolean
  motivos: { codigo: string; mensaje: string }[]
  unidadesActivas: number
  unidadesMaximas: number
  unidadesDisponiblesParaPrestar: number
  prestamosActivos: number
  prestamosVencidos: number
  sancionVigente: { fechaInicio: string; fechaFin: string } | null
}

export type LoanState = 'VIGENTE' | 'DEVOLUCION_PARCIAL' | 'VENCIDO' | 'FINALIZADO'
export type LoanReference = { id: string; codigo: string; estado?: LoanState }
export type UserReference = { id: string; nombres: string; apellidos: string; identificacion?: string }
export type UnitReference = { id: string; codigoInventario: string; nombre: string; estado?: InventoryState }
export type LoanDetail = { id: string; implemento: UnitReference; estado: 'PENDIENTE_DEVOLUCION' | 'DEVUELTO' | 'RESUELTO_POR_INCIDENCIA' }
export type LoanSummary = { id: string; codigo: string; estado: LoanState; usuario: UserReference; fechaPrestamo: string; fechaLimiteDevolucion: string; vencido: boolean; cantidadImplementos: number }
export type Loan = LoanSummary & { prestador: UserReference; fechaFinalizacion: string | null; unidadesPendientes: number; detalles: LoanDetail[]; devoluciones?: { id: string; codigo: string; fechaDevolucion: string }[]; incidencias?: { id: string; codigo: string; tipo: IncidentType; codigoInventario?: string }[] }
export type CreateLoanRequest = { usuarioId: string; implementoIds: string[] }

export type Condition = 'BUENO' | 'CON_NOVEDAD'
export type IncidentType = 'DANO' | 'PERDIDA' | 'ROBO' | 'OTRA'
export type ReturnScope = 'PARCIAL' | 'COMPLETA'
export type ReturnItemRequest = { implementoId: string; condicion: Condition; observaciones?: string; incidencia?: { tipo: Extract<IncidentType, 'DANO' | 'OTRA'>; descripcion: string } }
export type CreateReturnRequest = { items: ReturnItemRequest[]; retrasoJustificado?: boolean; justificacion?: string; observaciones?: string }
export type ReturnSummary = { id: string; codigo: string; prestamo: LoanReference; usuario: UserReference; fechaDevolucion: string; alcance: ReturnScope; cantidadImplementos: number; conNovedad: boolean; implementos: UnitReference[] }
export type ReturnDetail = ReturnSummary & { prestador: UserReference; retrasoJustificado: boolean; justificacion?: string | null; observaciones?: string | null; items: { implemento: UnitReference; condicion: Condition; observaciones?: string | null; estadoImplementoResultante: InventoryState; incidencia?: IncidentSummary | null }[]; sancion?: { fechaInicio: string; fechaFin: string; minutosRetraso: number } | null }

export type IncidentStatus = 'REGISTRADA' | 'RESUELTA'
export type IncidentSummary = { id: string; codigo: string; tipo: IncidentType; estado: IncidentStatus; fechaRegistro: string; fechaResolucion?: string | null; prestamo: LoanReference; implemento: UnitReference; usuario: UserReference }
export type Incident = IncidentSummary & { descripcion: string; prestador: UserReference; resueltoPor?: UserReference | null }
export type CreateIncidentRequest = { implementoId: string; tipo: Extract<IncidentType, 'PERDIDA' | 'ROBO'>; descripcion: string }

export type ApiResult<T> = { data: T; message?: string }
