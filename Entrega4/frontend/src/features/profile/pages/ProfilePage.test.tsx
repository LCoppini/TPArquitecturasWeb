import { screen } from '@testing-library/react'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { mockApi, signInAs } from '@/test/mockApi'
import { jsonResponse, renderWithApp } from '@/test/render'
import { ProfilePage } from './ProfilePage'

describe('ProfilePage', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('muestra los datos de la cuenta, incluido el correo y el rol', async () => {
    mockApi({
      'GET /usuarios/me': { idUsuario: 3, dni: 20000003, nombre: 'Valentina', apellido: 'Ruiz', ciudad: 'Tandil', email: 'cliente@ticketya.dev', roles: [{ idRol: 3, tipo: 'CLIENTE' }] },
    })
    renderWithApp(<ProfilePage />)
    expect(await screen.findByText('Valentina Ruiz')).toBeInTheDocument()
    expect(screen.getByText('cliente@ticketya.dev')).toBeInTheDocument()
    expect(screen.getByText('20000003')).toBeInTheDocument()
    expect(screen.getByText('Tandil')).toBeInTheDocument()
    expect(screen.getByText('Cliente')).toBeInTheDocument()
  })

  it('si falla ofrece reintentar', async () => {
    mockApi({ 'GET /usuarios/me': () => jsonResponse({ message: 'caído' }, 500) })
    renderWithApp(<ProfilePage />)
    expect(await screen.findByText('No pudimos cargar tu perfil')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})
