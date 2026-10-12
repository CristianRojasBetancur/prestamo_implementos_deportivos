import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('prototipo del sistema de préstamos', () => {
  it('muestra el catálogo público y filtra los implementos por texto', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Implementos deportivos' })).toBeInTheDocument()
    expect(screen.getByText('Balón de fútbol')).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: 'Buscar implementos' }), { target: { value: 'cuerda' } })
    expect(screen.getByText('Cuerda para saltar')).toBeInTheDocument()
    expect(screen.queryByText('Balón de fútbol')).not.toBeInTheDocument()
  })

  it('permite seleccionar el rol y abrir la sesión de demostración', () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: /acceso institucional/i }))
    fireEvent.click(screen.getByRole('button', { name: 'Administrador' }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'admin@institucion.edu.co' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'demo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    expect(screen.getByRole('navigation', { name: 'Navegación principal' })).toBeInTheDocument()
    expect(within(screen.getByRole('navigation', { name: 'Navegación principal' })).getByRole('button', { name: /inventario/i })).toBeInTheDocument()
  })

  it('muestra unidades al entrar al inventario y permite filtrar por categoría', () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: /acceso institucional/i }))
    fireEvent.click(screen.getByRole('button', { name: 'Administrador' }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'demo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    fireEvent.click(within(screen.getByRole('navigation', { name: 'Navegación principal' })).getByRole('button', { name: /inventario/i }))
    expect(screen.getByText('BAL-001')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Categoría'), { target: { value: 'Raquetas' } })
    expect(screen.getByText('RAQ-007')).toBeInTheDocument()
    expect(screen.queryByText('BAL-001')).not.toBeInTheDocument()
  })

  it('recorre los pasos del préstamo según el prototipo', () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: /acceso institucional/i }))
    fireEvent.change(screen.getByLabelText('Correo o identificación'), { target: { value: 'prestador' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'demo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
    fireEvent.click(screen.getByRole('button', { name: /registrar préstamo/i }))
    fireEvent.click(screen.getByRole('button', { name: /continuar con este usuario/i }))
    expect(screen.getByText('Paso 2 de 3')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /continuar con 2 implementos/i }))
    expect(screen.getByText('Paso 3 de 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar préstamo' })).toBeInTheDocument()
  })
})
