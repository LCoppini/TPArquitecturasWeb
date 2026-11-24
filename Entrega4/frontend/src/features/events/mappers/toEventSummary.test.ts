import type { EventoApi, EventoPaginaApi } from '../types/event'
import { toEventPage, toEventSummary } from './toEventSummary'

const evento = (override: Partial<EventoApi> = {}): EventoApi => ({
  id: 3,
  idLugar: 1,
  idOrganizador: 1,
  nombre: 'Noche de rock',
  descripcion: null,
  tipo: 'RECITAL',
  fechaHora: '2026-12-01T23:00:00Z',
  estado: 'PUBLICADO',
  participantes: [],
  sectores: [
    { idSector: 1, precio: 48500 },
    { idSector: 2, precio: 32000 },
  ],
  ...override,
})

describe('toEventSummary', () => {
  it('toma el menor precio de los sectores', () => {
    expect(toEventSummary(evento()).fromPrice).toBe(32000)
  })

  it('deja el precio en null si no hay sectores', () => {
    expect(toEventSummary(evento({ sectores: [] })).fromPrice).toBeNull()
  })

  it.each([
    ['PUBLICADO', 'available'],
    ['AGOTADO', 'soldout'],
    ['CANCELADO', 'cancelled'],
    ['SUSPENDIDO', 'suspended'],
    ['FINALIZADO', 'finished'],
    ['OTRO', 'available'],
  ])('mapea el estado %s a %s', (estado, status) => {
    expect(toEventSummary(evento({ estado })).status).toBe(status)
  })

  it('traduce la categoría conocida y da formato a la desconocida', () => {
    expect(toEventSummary(evento()).category).toBe('Recitales')
    expect(toEventSummary(evento({ tipo: 'CONFERENCIA' })).category).toBe('Conferencia')
  })

  it('no inventa la sede', () => {
    expect(toEventSummary(evento()).venue).toBeUndefined()
  })
})

describe('toEventPage', () => {
  it('pasa la página de base 0 a base 1', () => {
    const pagina: EventoPaginaApi = { contenido: [evento()], pagina: 0, tamano: 8, totalElementos: 1, totalPaginas: 1 }
    expect(toEventPage(pagina)).toMatchObject({ page: 1, totalPages: 1, totalItems: 1 })
  })
})
