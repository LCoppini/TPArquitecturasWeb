import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { jsonResponse, renderWithApp } from '@/test/render'
import { RegisterForm } from './RegisterForm'

const fill = async (password = 'secreta123') => {
  await userEvent.type(screen.getByLabelText('Nombre'), 'Valentina')
  await userEvent.type(screen.getByLabelText('Apellido'), 'Ruiz')
  await userEvent.type(screen.getByLabelText('DNI'), '30123456')
  await userEvent.type(screen.getByLabelText('Ciudad'), 'Tandil')
  await userEvent.type(screen.getByLabelText('Correo electrónico'), 'vale@correo.com')
  await userEvent.type(screen.getByLabelText('Contraseña'), password)
  await userEvent.click(screen.getByRole('checkbox'))
  await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
}

describe('RegisterForm', () => {
  const fetchMock = vi.fn()
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('rechaza una contraseña corta sin llamar al backend', async () => {
    renderWithApp(<RegisterForm />)
    await fill('corta')
    expect(await screen.findByText('Usá al menos 8 caracteres.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('avisa cuando el correo o el DNI ya están registrados (409)', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ message: 'duplicado' }, 409))
    renderWithApp(<RegisterForm />)
    await fill()
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya hay una cuenta con esos datos')
  })

  it('registra, ingresa y guarda la sesión', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ id: 1 }, 201))
      .mockResolvedValueOnce(jsonResponse({ token: makeToken({ exp: secondsFromNow(600) }) }))
    renderWithApp(<RegisterForm />)
    await fill()
    await waitFor(() => expect(sessionStorage.getItem('ticketya.session')).toContain('vale@correo.com'))
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/usuarios/registro')
    expect(JSON.parse(init.body as string)).toMatchObject({ dni: 30123456, ciudad: 'Tandil' })
  })
})
