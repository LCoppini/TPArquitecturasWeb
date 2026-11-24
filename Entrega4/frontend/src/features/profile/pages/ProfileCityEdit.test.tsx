import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { makeToken, secondsFromNow } from '@/test/jwt'
import { callsTo, mockApi, signInAs } from '@/test/mockApi'
import { jsonResponse, renderWithApp } from '@/test/render'
import { ProfilePage } from './ProfilePage'

const ME = { idUsuario: 3, dni: 20000003, nombre: 'Valentina', apellido: 'Ruiz', ciudad: 'Tandil', email: 'cliente@ticketya.dev', roles: [{ idRol: 3, tipo: 'CLIENTE' }] }

describe('ProfilePage · cambio de ciudad', () => {
  beforeEach(() => signInAs(makeToken({ exp: secondsFromNow(600) })))
  afterEach(() => vi.unstubAllGlobals())

  it('guarda la nueva ciudad y muestra el dato actualizado', async () => {
    const api = mockApi({
      'GET /usuarios/me': ME,
      'PUT /usuarios/me/ciudad': ({ body }: { body: unknown }) => jsonResponse({ ...ME, ciudad: (body as { ciudad: string }).ciudad }),
    })
    renderWithApp(<ProfilePage />)
    const input = await screen.findByLabelText('Ciudad')
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeDisabled()
    await userEvent.clear(input)
    await userEvent.type(input, '  Rosario ')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('Guardamos tu ciudad.')).toBeInTheDocument()
    expect(callsTo(api, 'PUT', '/usuarios/me/ciudad')).toHaveLength(1)
    expect(JSON.parse(callsTo(api, 'PUT', '/usuarios/me/ciudad')[0][1]?.body as string)).toEqual({ ciudad: 'Rosario' })
    expect(await screen.findByText('Rosario')).toBeInTheDocument()
  })

  it('no permite guardar una ciudad vacía', async () => {
    const api = mockApi({ 'GET /usuarios/me': ME })
    renderWithApp(<ProfilePage />)
    await userEvent.clear(await screen.findByLabelText('Ciudad'))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('Ingresá tu ciudad.')).toBeInTheDocument()
    expect(callsTo(api, 'PUT', '/usuarios/me/ciudad')).toHaveLength(0)
  })

  it('si el servidor rechaza el cambio lo informa', async () => {
    mockApi({ 'GET /usuarios/me': ME, 'PUT /usuarios/me/ciudad': () => jsonResponse({ message: 'Ciudad inválida' }, 400) })
    renderWithApp(<ProfilePage />)
    const input = await screen.findByLabelText('Ciudad')
    await userEvent.type(input, ' Sur')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(await screen.findByText('No pudimos guardar tu ciudad')).toBeInTheDocument()
  })
})
