import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { jsonResponse, renderWithApp } from '@/test/render'
import { HomePage } from './HomePage'

const page = (contenido: object[], extra = {}) => ({
  contenido,
  pagina: 0,
  tamano: 8,
  totalElementos: contenido.length,
  totalPaginas: 1,
  ...extra,
})

const evento = (id: number, nombre: string) => ({
  id, idLugar: 1, idOrganizador: 1, nombre, descripcion: null, tipo: 'RECITAL',
  fechaHora: '2026-12-01T23:00:00Z', estado: 'PUBLICADO', participantes: [], sectores: [{ idSector: 1, precio: 1000 }],
})

describe('HomePage', () => {
  const fetchMock = vi.fn()
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('muestra los eventos devueltos por el backend', async () => {
    fetchMock.mockResolvedValue(jsonResponse(page([evento(1, 'Noche de rock'), evento(2, 'Obra de teatro')])))
    renderWithApp(<HomePage />)
    expect(await screen.findByRole('heading', { name: 'Noche de rock' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Obra de teatro' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('2 eventos')
  })

  it('pide la categoría elegida al backend', async () => {
    fetchMock.mockResolvedValue(jsonResponse(page([evento(1, 'Noche de rock')])))
    renderWithApp(<HomePage />)
    await screen.findByRole('heading', { name: 'Noche de rock' })
    await userEvent.click(screen.getByRole('button', { name: 'Teatro' }))
    await screen.findByRole('button', { name: 'Teatro', pressed: true })
    const urls = fetchMock.mock.calls.map(([url]) => url as string)
    expect(urls.at(-1)).toContain('tipo=TEATRO')
  })

  it('ofrece limpiar filtros cuando no hay resultados', async () => {
    fetchMock.mockResolvedValue(jsonResponse(page([], { totalPaginas: 0 })))
    renderWithApp(<HomePage />, '/?categoria=teatro')
    expect(await screen.findByText('No hay eventos para esta búsqueda')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Limpiar filtros' })).toBeInTheDocument()
  })

  it('muestra el error y permite reintentar', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({}, 500)).mockResolvedValueOnce(jsonResponse(page([evento(1, 'Noche de rock')])))
    renderWithApp(<HomePage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { name: 'Noche de rock' })).toBeInTheDocument()
  })
})
