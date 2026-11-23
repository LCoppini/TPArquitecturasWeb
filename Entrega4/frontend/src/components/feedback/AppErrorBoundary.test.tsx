import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AppErrorBoundary } from './AppErrorBoundary'

let shouldFail = true
const Fragile = () => {
  if (shouldFail) throw new Error('boom')
  return <p>Todo bien</p>
}

describe('AppErrorBoundary', () => {
  beforeEach(() => {
    shouldFail = true
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
  })
  afterEach(() => vi.restoreAllMocks())

  it('muestra el error general en lugar de una pantalla en blanco', () => {
    render(
      <AppErrorBoundary>
        <Fragile />
      </AppErrorBoundary>,
    )
    expect(screen.getByText('Algo salió de nuestro lado')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })

  it('al reintentar vuelve a dibujar la app si el problema ya pasó', async () => {
    render(
      <AppErrorBoundary>
        <Fragile />
      </AppErrorBoundary>,
    )
    shouldFail = false
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(screen.getByText('Todo bien')).toBeInTheDocument()
  })

  it('sin errores deja pasar el contenido', () => {
    shouldFail = false
    render(
      <AppErrorBoundary>
        <Fragile />
      </AppErrorBoundary>,
    )
    expect(screen.getByText('Todo bien')).toBeInTheDocument()
  })
})
