import { screen } from '@testing-library/react'
import { AppRoutes } from '@/app/AppRoutes'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { mockApi, signInAs } from '@/test/mockApi'
import { renderWithApp } from '@/test/render'

describe('rutas protegidas y sesión vencida', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('sin sesión, Mis entradas lleva a ingresar', async () => {
    mockApi({})
    renderWithApp(<AppRoutes />, '/mis-entradas')
    expect(await screen.findByText('Ingresá a tu cuenta')).toBeInTheDocument()
  })

  it('con sesión permite entrar a Mis entradas y al perfil desde el menú', async () => {
    signInAs(makeToken({ exp: secondsFromNow(600) }))
    mockApi({ 'GET /entradas': [] })
    renderWithApp(<AppRoutes />, '/mis-entradas')
    expect(await screen.findByRole('heading', { name: 'Mis entradas' })).toBeInTheDocument()
  })

  it('cuando el servidor rechaza el token (401) avisa que la sesión venció y pide ingresar de nuevo', async () => {
    signInAs(makeToken({ exp: secondsFromNow(600) }))
    mockApi({ 'GET /entradas': () => new Response(JSON.stringify({ message: 'vencido' }), { status: 401 }) })
    renderWithApp(<AppRoutes />, '/mis-entradas')
    expect(await screen.findByText('Tu sesión venció')).toBeInTheDocument()
    expect(await screen.findByText('Ingresá a tu cuenta')).toBeInTheDocument()
  })
})
