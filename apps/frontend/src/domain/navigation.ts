export type SectionId = 'resumen' | 'disponibilidad' | 'prestamos' | 'devoluciones' | 'inventario' | 'prestadores'

export const sections: { id: SectionId; label: string; icon: string; group: 'general' | 'gestion' }[] = [
  { id: 'resumen', label: 'Resumen', icon: '⌂', group: 'general' },
  { id: 'disponibilidad', label: 'Disponibilidad', icon: '◉', group: 'general' },
  { id: 'prestamos', label: 'Préstamos', icon: '↗', group: 'gestion' },
  { id: 'devoluciones', label: 'Devoluciones', icon: '↙', group: 'gestion' },
  { id: 'inventario', label: 'Inventario', icon: '▦', group: 'gestion' },
  { id: 'prestadores', label: 'Prestadores', icon: '♙', group: 'gestion' },
]
