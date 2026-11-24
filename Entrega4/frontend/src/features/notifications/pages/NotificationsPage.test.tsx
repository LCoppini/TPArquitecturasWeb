import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { mockApi, signInAs } from '@/test/mockApi'
import { jsonResponse, renderWithApp } from '@/test/render'
import { NotificationsPage } from './NotificationsPage'

const LISTA = [
  { id: 3, tipo: 'DEVOLUCION_APROBADA', asunto: 'Tu devolución fue aprobada', estado: 'ENVIADA', fechaCreacion: '2026-11-22T21:53:00Z', fechaEnvio: '2026-11-22T21:53:10Z' },
  { id: 2, tipo: 'EVENTO_CANCELADO', asunto: 'El evento fue cancelado', estado: 'PENDIENTE', fechaCreacion: '2026-11-21T12:00:00Z', fechaEnvio: null },
  { id: 1, tipo: 'COMPRA_CONFIRMADA', asunto: 'Tu compra está confirmada', estado: 'FALLIDA', fechaCreacion: '2026-11-20T09:30:00Z', fechaEnvio: null },
]

describe('NotificationsPage', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('lista los avisos con su tipo, asunto y estado de envío', async () => {
    mockApi({ 'GET /notificaciones/mias': LISTA })
    renderWithApp(<NotificationsPage />)
    expect(await screen.findByRole('heading', { name: 'Devolución aprobada' })).toBeInTheDocument()
    expect(screen.getByText('Tu compra está confirmada')).toBeInTheDocument()
    expect(screen.getByText('Enviada')).toBeInTheDocument()
    expect(screen.getByText('Pendiente')).toBeInTheDocument()
    expect(screen.getByText('Fallida')).toBeInTheDocument()
  })

  it('permite filtrar por estado de envío', async () => {
    mockApi({ 'GET /notificaciones/mias': LISTA })
    renderWithApp(<NotificationsPage />)
    await userEvent.click(await screen.findByRole('button', { name: /Fallidas/ }))
    expect(screen.getByRole('heading', { name: 'Compra confirmada' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Devolución aprobada' })).not.toBeInTheDocument()
  })

  it('sin avisos lo explica', async () => {
    mockApi({ 'GET /notificaciones/mias': [] })
    renderWithApp(<NotificationsPage />)
    expect(await screen.findByText('Todavía no tenés notificaciones')).toBeInTheDocument()
  })

  it('si una categoría está vacía lo dice sin ocultar los filtros', async () => {
    mockApi({ 'GET /notificaciones/mias': [LISTA[0]] })
    renderWithApp(<NotificationsPage />)
    await userEvent.click(await screen.findByRole('button', { name: /Fallidas/ }))
    expect(screen.getByText('No hay notificaciones en esta categoría')).toBeInTheDocument()
  })

  it('si falla ofrece reintentar', async () => {
    mockApi({ 'GET /notificaciones/mias': () => jsonResponse({ message: 'caído' }, 500) })
    renderWithApp(<NotificationsPage />)
    expect(await screen.findByText('No pudimos cargar tus notificaciones')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
