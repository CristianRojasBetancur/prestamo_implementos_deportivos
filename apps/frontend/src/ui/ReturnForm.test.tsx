import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Loan } from '../services/api/types'
import { ReturnFormPartial } from './ReturnForm'

const loan: Loan = {
  id: 'loan-1',
  codigo: 'PRE-000001',
  estado: 'VIGENTE',
  usuario: { id: 'user-1', nombres: 'Laura', apellidos: 'Gómez', identificacion: '102334455' },
  prestador: { id: 'provider-1', nombres: 'María', apellidos: 'Rodríguez' },
  fechaPrestamo: '2026-11-20T08:00:00-05:00',
  fechaLimiteDevolucion: '2026-11-21T18:00:00-05:00',
  fechaFinalizacion: null,
  vencido: false,
  cantidadImplementos: 2,
  unidadesPendientes: 2,
  detalles: [
    { id: 'detail-1', implemento: { id: 'unit-1', codigoInventario: 'BAL-001', nombre: 'Balón de fútbol' }, estado: 'PENDIENTE_DEVOLUCION' },
    { id: 'detail-2', implemento: { id: 'unit-2', codigoInventario: 'CON-002', nombre: 'Cono deportivo' }, estado: 'PENDIENTE_DEVOLUCION' },
  ],
}

const renderForm = (onSubmit = vi.fn(() => Promise.resolve())) => render(
  <ReturnFormPartial loan={loan} busy={false} error="" onCancel={vi.fn()} onSubmit={onSubmit} />,
)

describe('ReturnFormPartial', () => {
  it('selecciona todas las unidades inicialmente y excluye las desmarcadas del payload', async () => {
    const onSubmit = vi.fn(() => Promise.resolve())
    renderForm(onSubmit)

    const checkboxes = screen.getAllByRole('checkbox')
    expect(checkboxes).toHaveLength(2)
    expect(checkboxes[0]).toBeChecked()
    expect(checkboxes[1]).toBeChecked()

    fireEvent.click(screen.getByRole('checkbox', { name: /cono deportivo/i }))
    const disabledRow = screen.getByText('Cono deportivo').closest('.return-item')
    expect(disabledRow).toHaveClass('disabled')
    expect(within(disabledRow as HTMLElement).getByRole('combobox')).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar devolución' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      items: [{ implementoId: 'unit-1', condicion: 'BUENO', incidencia: undefined }],
      retrasoJustificado: false,
      justificacion: undefined,
    }))
  })

  it('deshabilita el registro cuando no hay unidades seleccionadas', () => {
    renderForm()
    fireEvent.click(screen.getByRole('checkbox', { name: /balón de fútbol/i }))
    fireEvent.click(screen.getByRole('checkbox', { name: /cono deportivo/i }))

    expect(screen.getByRole('status')).toHaveTextContent('Selecciona al menos una unidad')
    expect(screen.getByRole('button', { name: 'Confirmar devolución' })).toBeDisabled()
  })

  it('envía la condición y la incidencia únicamente de la unidad seleccionada', async () => {
    const onSubmit = vi.fn(() => Promise.resolve())
    renderForm(onSubmit)

    fireEvent.click(screen.getByRole('checkbox', { name: /balón de fútbol/i }))
    const selectedRow = screen.getByText('Cono deportivo').closest('.return-item') as HTMLElement
    fireEvent.change(within(selectedRow).getByRole('combobox'), { target: { value: 'CON_NOVEDAD' } })
    fireEvent.change(screen.getByRole('textbox', { name: /descripción de cono deportivo/i }), { target: { value: 'Tiene una grieta.' } })
    fireEvent.click(screen.getByRole('checkbox', { name: /cono deportivo/i }))
    fireEvent.click(screen.getByRole('checkbox', { name: /cono deportivo/i }))
    fireEvent.click(screen.getByRole('button', { name: /registrar devolución e incidencia/i }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      items: [{
        implementoId: 'unit-2',
        condicion: 'CON_NOVEDAD',
        incidencia: { tipo: 'DANO', descripcion: 'Tiene una grieta.' },
      }],
      retrasoJustificado: false,
      justificacion: undefined,
    }))
  })
})
