import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { entrada, eventoDetalle } from '@/test/fixtures'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { callsTo, mockApi, signInAs } from '@/test/mockApi'
import { jsonResponse, renderWithApp } from '@/test/render'
import { MyTicketsPage } from './MyTicketsPage'

const BASE = { 'GET /eventos/1/detalle': eventoDetalle() }

describe('MyTicketsPage', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('muestra las entradas agrupadas por compra con evento, sector, asiento y QR', async () => {
    mockApi({
      ...BASE,
      'GET /entradas': [entrada(), entrada({ id: 12, asiento: 3, codigoQr: 'qr-uuid-12' }), entrada({ id: 20, idCompra: 6, idSector: 1, fila: null, asiento: null })],
    })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByRole('heading', { name: 'Compra #5' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Compra #6' })).toBeInTheDocument()
    expect((await screen.findAllByText('Noche de Rock Nacional')).length).toBe(3)
    expect(screen.getAllByText(/Platea Baja · Fila 1 · Asiento/)).toHaveLength(2)
    expect(screen.getByText(/Campo · Ubicación general/)).toBeInTheDocument()
    expect(screen.getAllByRole('img', { name: 'Código QR de la entrada' })).toHaveLength(3)
  })

  it('no muestra QR en entradas que ya no son válidas y las etiqueta con su estado', async () => {
    mockApi({
      ...BASE,
      'GET /entradas': [entrada({ estado: 'USADA' }), entrada({ id: 12, estado: 'DEVUELTA', estadoDevolucion: 'APROBADA' })],
    })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByText('Usada')).toBeInTheDocument()
    expect(screen.getByText('Devuelta')).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: 'Código QR de la entrada' })).not.toBeInTheDocument()
  })

  it('sin entradas invita a explorar eventos', async () => {
    mockApi({ 'GET /entradas': [] })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByText('Todavía no tenés entradas')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Explorar eventos' })).toHaveAttribute('href', '/')
  })

  it('pide confirmación antes de devolver una compra y luego llama al backend', async () => {
    const api = mockApi({
      ...BASE,
      'GET /entradas': [entrada()],
      'POST /compras/5/devolucion': () => new Response(null, { status: 202 }),
    })
    renderWithApp(<MyTicketsPage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Devolver compra' }))
    expect(callsTo(api, 'POST', '/compras/5/devolucion')).toHaveLength(0)
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar devolución' }))
    expect(await screen.findByRole('button', { name: 'Devolver compra' })).toBeInTheDocument()
    expect(callsTo(api, 'POST', '/compras/5/devolucion')).toHaveLength(1)
  })

  it('si la devolución no corresponde muestra el motivo del backend', async () => {
    mockApi({
      ...BASE,
      'GET /entradas': [entrada()],
      'POST /compras/5/devolucion': () => jsonResponse({ message: 'Faltan menos de 72 horas para el evento' }, 409),
    })
    renderWithApp(<MyTicketsPage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Devolver compra' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar devolución' }))
    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText(/72 horas/)).toBeInTheDocument()
  })

  it('con devolución pendiente avisa que está en proceso y no ofrece devolver de nuevo', async () => {
    mockApi({ ...BASE, 'GET /entradas': [entrada({ estado: 'EN_REINTEGRO', estadoDevolucion: 'PENDIENTE' })] })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByText('Devolución solicitada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Devolver compra' })).not.toBeInTheDocument()
  })
})
