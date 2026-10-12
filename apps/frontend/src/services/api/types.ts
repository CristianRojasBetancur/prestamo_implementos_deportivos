export type Category = { id: string; name: string }
export type AvailabilityItem = { id: string; name: string; category: string; availableUnits: number }
export type ApiResult<T> = { data: T; message?: string }
