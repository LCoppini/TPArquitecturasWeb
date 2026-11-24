import { screen, waitFor } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { disponibilidad, eventoDetalle } from '@/test/fixtures'
import { mockApi } from '@/test/mockApi'
import { renderWithApp } from '@/test/render'
import { EventDetailPage } from './EventDetailPage'

const renderDetail = () =>
  renderWithApp(
    <Routes>
      <Route path="eventos/:id" element={<EventDetailPage />} />
    </Routes>,
    '/eventos/1',
  )

describe('EventDetailPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('muestra el evento con su sede, participantes y sectores con disponibilidad', async () => {
    mockApi({
      'GET /eventos/1/detalle': eventoDetalle(),
      'GET /eventos/1/disponibilidad': () => new Response(JSON.stringify(disponibilidad(1, 12)), { status: 200 }),
    })
    renderDetail()
    expect(await screen.findByRole('heading', { level: 1, name: 'Noche de Rock Nacional' })).toBeInTheDocument()
    expect(screen.getByText(/Estadio Central/)).toBeInTheDocument()
    expect(screen.getByText('Los Fugitivos')).toBeInTheDocument()
    await waitFor(() => expect(screen.getAllByText('12 disponibles')).toHaveLength(2))
    expect(screen.getByRole('radio', { name: /Campo/ })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Platea Baja/ })).toBeInTheDocument()
  })

  it('un evento agotado se muestra con su estado y sin panel de compra', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle({ estado: 'AGOTADO' }), 'GET /eventos/1/disponibilidad': disponibilidad(1, 0) })
    renderDetail()
    expect(await screen.findByText('Agotado', { selector: 'span' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /fila/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Ingresá para comprar' })).not.toBeInTheDocument()
  })

  it('un evento cancelado muestra su estado', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle({ estado: 'CANCELADO' }) })
    renderDetail()
    expect(await screen.findByText('Cancelado')).toBeInTheDocument()
  })

  it('sin sesión invita a ingresar para comprar', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle(), 'GET /eventos/1/disponibilidad': disponibilidad(1, 5) })
    renderDetail()
    expect(await screen.findByRole('link', { name: 'Ingresá para comprar' })).toHaveAttribute('href', '/ingresar')
  })

  it('un evento inexistente muestra un mensaje claro', async () => {
    mockApi({})
    renderDetail()
    expect(await screen.findByText('No encontramos este evento')).toBeInTheDocument()
  })
})
