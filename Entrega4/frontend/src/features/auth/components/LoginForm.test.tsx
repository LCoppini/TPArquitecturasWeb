import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { jsonResponse, renderWithApp } from '@/test/render'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { LoginForm } from './LoginForm'

const fillAndSubmit = async (email: string, password: string) => {
  await userEvent.type(screen.getByLabelText('Correo electrónico'), email)
  await userEvent.type(screen.getByLabelText('Contraseña'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
}

describe('LoginForm', () => {
  const fetchMock = vi.fn()
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('valida los campos antes de llamar al backend', async () => {
    renderWithApp(<LoginForm />)
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Ingresá tu correo electrónico.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('muestra un aviso claro ante credenciales incorrectas (401)', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ message: 'Credenciales inválidas' }, 401))
    renderWithApp(<LoginForm />)
    await fillAndSubmit('a@b.com', 'mala')
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos')
  })

  it('guarda la sesión cuando el ingreso es correcto', async () => {
    const token = makeToken({ exp: secondsFromNow(600) })
    fetchMock.mockResolvedValue(jsonResponse({ token }))
    renderWithApp(<LoginForm />)
    await fillAndSubmit('a@b.com', 'secreta123')
    await waitFor(() => expect(sessionStorage.getItem('ticketya.session')).toContain('a@b.com'))
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/usuarios/login')
    expect(JSON.parse(init.body as string)).toEqual({ email: 'a@b.com', password: 'secreta123' })
  })
})
