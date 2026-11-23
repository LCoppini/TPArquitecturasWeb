import { screen } from '@testing-library/react'
import { eventoDetalle } from '@/test/fixtures'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { mockApi, signInAs } from '@/test/mockApi'
import { renderWithApp } from '@/test/render'
import { RecommendedEvents } from './RecommendedEvents'

describe('RecommendedEvents', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('con sesión muestra los eventos recomendados con su motivo', async () => {
    signInAs(makeToken({ idUsuario: 3, exp: secondsFromNow(600) }))
    mockApi({
      'GET /recomendaciones/clientes/3': [{ idEvento: 1, fechaHora: '2027-01-10T23:00:00Z', tipo: 'Recital', motivo: 'Porque te gusta Los Fugitivos' }],
      'GET /eventos/1/detalle': eventoDetalle(),
    })
    renderWithApp(<RecommendedEvents />)
    expect(await screen.findByRole('link', { name: 'Noche de Rock Nacional' })).toHaveAttribute('href', '/eventos/1')
    expect(screen.getByText('Porque te gusta Los Fugitivos')).toBeInTheDocument()
  })

  it('sin sesión no consulta ni muestra nada', () => {
    const api = mockApi({})
    renderWithApp(<RecommendedEvents />)
    expect(api).not.toHaveBeenCalled()
    expect(screen.queryByText('Recomendados para vos')).not.toBeInTheDocument()
  })

  it('si no hay recomendaciones no muestra la sección', async () => {
    signInAs(makeToken({ idUsuario: 3, exp: secondsFromNow(600) }))
    const api = mockApi({ 'GET /recomendaciones/clientes/3': [] })
    renderWithApp(<RecommendedEvents />)
    await vi.waitFor(() => expect(api).toHaveBeenCalled())
    expect(screen.queryByText('Recomendados para vos')).not.toBeInTheDocument()
  })
})
