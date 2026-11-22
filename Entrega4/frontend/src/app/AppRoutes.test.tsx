import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { renderWithApp } from '@/test/render'
import { AppRoutes } from './AppRoutes'

const navbar = () => within(screen.getByRole('navigation', { name: 'Principal' }))

describe('AppRoutes', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
  })
  afterEach(() => vi.unstubAllGlobals())

  it('una ruta desconocida muestra la pantalla 404', () => {
    renderWithApp(<AppRoutes />, '/no-existe')
    expect(screen.getByText('No encontramos esta página')).toBeInTheDocument()
  })

  it('sin sesión la barra ofrece ingresar y crear cuenta', () => {
    renderWithApp(<AppRoutes />)
    expect(navbar().getByRole('link', { name: 'Ingresar' })).toHaveAttribute('href', '/ingresar')
    expect(navbar().getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/registro')
  })

  it('con sesión la barra muestra el menú de cuenta y permite cerrar sesión', async () => {
    sessionStorage.setItem('ticketya.session', JSON.stringify({ token: makeToken({ exp: secondsFromNow(600) }), email: 'vale@correo.com' }))
    renderWithApp(<AppRoutes />)
    expect(navbar().queryByRole('link', { name: 'Ingresar' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Menú de tu cuenta' }))
    expect(screen.getByText('vale@correo.com')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }))
    expect(navbar().getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
    expect(sessionStorage.getItem('ticketya.session')).toBeNull()
  })

  it('el enlace de la categoría activa queda marcado', () => {
    renderWithApp(<AppRoutes />, '/?categoria=teatro')
    expect(screen.getByRole('navigation', { name: 'Principal' }).querySelector('[aria-current="page"]')).toHaveTextContent('Teatro')
  })
})
