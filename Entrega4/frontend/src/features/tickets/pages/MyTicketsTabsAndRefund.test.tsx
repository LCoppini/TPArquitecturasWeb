import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { entrada, eventoDetalle } from '@/test/fixtures'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { mockApi, signInAs } from '@/test/mockApi'
import { renderWithApp } from '@/test/render'
import { MyTicketsPage } from './MyTicketsPage'

const ENTRADAS = [
  entrada({ id: 11, idCompra: 5 }),
  entrada({ id: 12, idCompra: 6, estado: 'USADA' }),
  entrada({ id: 13, idCompra: 7, estado: 'DEVUELTA', estadoDevolucion: 'APROBADA', precio: 18000 }),
]

describe('MyTicketsPage · pestañas y estado de devolución', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('muestra todas las compras y deja filtrar por pestaña con su cantidad', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle(), 'GET /entradas': ENTRADAS })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByRole('heading', { name: 'Compra #5' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Compra #6' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Compra #7' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /Devueltas/ }))
    expect(screen.getByRole('heading', { name: 'Compra #7' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Compra #5' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /Pasadas/ }))
    expect(screen.getByRole('heading', { name: 'Compra #6' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Compra #7' })).not.toBeInTheDocument()
  })

  it('si una pestaña no tiene compras lo dice', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle(), 'GET /entradas': [entrada({ id: 12, estado: 'USADA' })] })
    renderWithApp(<MyTicketsPage />)
    await userEvent.click(await screen.findByRole('button', { name: /Devueltas/ }))
    expect(screen.getByText('No hay compras en esta sección')).toBeInTheDocument()
  })

  it('informa la devolución aprobada con lo reintegrado', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle(), 'GET /entradas': [ENTRADAS[2]] })
    renderWithApp(<MyTicketsPage />)
    const status = await screen.findByRole('status')
    expect(within(status).getByText('Devolución aprobada')).toBeInTheDocument()
    expect(within(status).getByText('Reintegramos $ 18.000.')).toBeInTheDocument()
  })

  it('informa la devolución rechazada', async () => {
    mockApi({ 'GET /eventos/1/detalle': eventoDetalle(), 'GET /entradas': [entrada({ estadoDevolucion: 'RECHAZADA' })] })
    renderWithApp(<MyTicketsPage />)
    expect(await screen.findByText('Devolución rechazada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Devolver compra' })).not.toBeInTheDocument()
  })
})
