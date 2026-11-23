import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { EventDetailPage } from '@/features/events/pages/EventDetailPage'
import { disponibilidad, eventoDetalle, ingreso } from '@/test/fixtures'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { callsTo, mockApi, signInAs } from '@/test/mockApi'
import { jsonResponse, renderWithApp } from '@/test/render'

const FILA = { idEvento: 1, estado: 'ACTIVA', maxPersonas: 5, cupoEntradas: 10 }
const BASE = {
  'GET /eventos/1/detalle': eventoDetalle(),
  'GET /eventos/1/disponibilidad': disponibilidad(1, 10),
}

const renderDetail = () =>
  renderWithApp(
    <Routes>
      <Route path="eventos/:id" element={<EventDetailPage />} />
    </Routes>,
    '/eventos/1',
  )

const pickCampo = async () => userEvent.click(await screen.findByRole('radio', { name: /Campo/ }, { timeout: 3000 }).then(async (radio) => {
  await waitFor(() => expect(radio).toBeEnabled())
  return radio
}))

describe('PurchasePanel', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('si el evento no tiene fila avisa que la venta no está habilitada', async () => {
    mockApi({ ...BASE })
    renderDetail()
    await pickCampo()
    expect(await screen.findByText('La venta todavía no está habilitada')).toBeInTheDocument()
  })

  it('permite entrar a la fila y muestra el lugar, las personas delante y la espera estimada', async () => {
    const api = mockApi({
      ...BASE,
      'GET /filas-virtuales/eventos/1': FILA,
      'GET /filas-virtuales/eventos/1/mi-estado': () => jsonResponse({ message: 'sin ingreso' }, 404),
      'POST /filas-virtuales/eventos/1/ingresos': ingreso(),
    })
    renderDetail()
    await pickCampo()
    await userEvent.click(await screen.findByRole('button', { name: 'Entrar a la fila para comprar' }))
    expect(await screen.findByText('#4')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('hasta 15 min')).toBeInTheDocument()
    expect(callsTo(api, 'POST', '/filas-virtuales/eventos/1/ingresos')).toHaveLength(1)
  })

  it('permite salir de la fila estando en espera', async () => {
    const api = mockApi({
      ...BASE,
      'GET /filas-virtuales/eventos/1': FILA,
      'GET /filas-virtuales/eventos/1/mi-estado': ingreso(),
      'DELETE /filas-virtuales/eventos/1/ingresos/mi': ingreso({ estado: 'SALIO', posicion: null }),
    })
    renderDetail()
    await pickCampo()
    await userEvent.click(await screen.findByRole('button', { name: 'Salir de la fila' }))
    expect(await screen.findByText('Saliste de la fila')).toBeInTheDocument()
    expect(callsTo(api, 'DELETE', '/filas-virtuales/eventos/1/ingresos/mi')).toHaveLength(1)
  })

  it('con turno habilitado reserva con la cantidad elegida y paga con éxito', async () => {
    const venceEn = new Date(Date.now() + 600_000).toISOString()
    const api = mockApi({
      ...BASE,
      'GET /filas-virtuales/eventos/1': FILA,
      'GET /filas-virtuales/eventos/1/mi-estado': ingreso({ estado: 'HABILITADO', posicion: 0, venceEn }),
      'POST /compras': () => jsonResponse({ idCompra: 5, idEvento: 1, estado: 'RESERVADA', total: 64000, venceEn, entradas: [], codigo: null }, 201),
      'POST /compras/5/pago': { idPago: 9, idCompra: 5, estado: 'PAGADA', total: 64000 },
    })
    renderDetail()
    await pickCampo()
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar una entrada' }))
    expect(await screen.findByText('¡Es tu turno!')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Reservar entradas' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Pagar' }))
    expect(await screen.findByText('¡Compra confirmada!')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver mis entradas' })).toHaveAttribute('href', '/mis-entradas')

    const [, reserva] = callsTo(api, 'POST', '/compras')[0] as [string, RequestInit]
    expect(JSON.parse(reserva.body as string).entradas).toEqual([{ idSector: 1 }, { idSector: 1 }])
    expect((reserva.headers as Record<string, string>)['Idempotency-Key']).toBeTruthy()
    const [, pago] = callsTo(api, 'POST', '/compras/5/pago')[0] as [string, RequestInit]
    expect(JSON.parse(pago.body as string)).toEqual({ medioDePago: 'Tarjeta de crédito' })
  })

  it('si el pago se rechaza lo informa y permite volver a intentar', async () => {
    const venceEn = new Date(Date.now() + 600_000).toISOString()
    mockApi({
      ...BASE,
      'GET /filas-virtuales/eventos/1': FILA,
      'GET /filas-virtuales/eventos/1/mi-estado': ingreso({ estado: 'HABILITADO', posicion: 0, venceEn }),
      'POST /compras': () => jsonResponse({ idCompra: 5, idEvento: 1, estado: 'RESERVADA', total: 32000, venceEn, entradas: [], codigo: null }, 201),
      'POST /compras/5/pago': { idPago: 9, idCompra: 5, estado: 'RECHAZADA', total: 32000 },
    })
    renderDetail()
    await pickCampo()
    await userEvent.click(await screen.findByRole('button', { name: 'Reservar entradas' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Pagar' }))
    expect(await screen.findByText('Pago rechazado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver a intentar' })).toBeInTheDocument()
  })

  it('si la reserva choca con una compra pendiente muestra el motivo', async () => {
    const venceEn = new Date(Date.now() + 600_000).toISOString()
    mockApi({
      ...BASE,
      'GET /filas-virtuales/eventos/1': FILA,
      'GET /filas-virtuales/eventos/1/mi-estado': ingreso({ estado: 'HABILITADO', posicion: 0, venceEn }),
      'POST /compras': () => jsonResponse({ codigo: 'COMPRA_PENDIENTE', entradas: [] }, 409),
    })
    renderDetail()
    await pickCampo()
    await userEvent.click(await screen.findByRole('button', { name: 'Reservar entradas' }))
    expect(await screen.findByText(/Ya tenés una compra en curso/)).toBeInTheDocument()
  })
})
