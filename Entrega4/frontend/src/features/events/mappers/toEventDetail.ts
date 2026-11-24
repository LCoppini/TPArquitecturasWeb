import { categoryLabelOf } from '../constants/categories'
import type { DisponibilidadApi, EventDetail, EventoDetalleApi, SectorAvailability } from '../types/detail'
import { toSaleStatus } from './toEventSummary'

export const toEventDetail = (evento: EventoDetalleApi): EventDetail => ({
  id: evento.id,
  title: evento.nombre,
  description: evento.descripcion,
  category: categoryLabelOf(evento.tipo),
  startsAt: evento.fechaHora,
  status: toSaleStatus(evento.estado),
  venue: { name: evento.lugar.nombre, address: evento.lugar.direccion, city: evento.lugar.ciudad },
  lineup: evento.participantes.map(({ id, nombre, tipo }) => ({ id, name: nombre, kind: tipo })),
  sectors: evento.sectores.map((sector) => ({
    id: sector.idSector,
    name: sector.nombre ?? `Sector ${sector.idSector}`,
    price: sector.precio,
    capacity: sector.capacidad,
  })),
})

export const toSectorAvailability = ({ disponibles, capacidad }: DisponibilidadApi): SectorAvailability => ({
  available: Math.max(disponibles, 0),
  capacity: capacidad,
})
