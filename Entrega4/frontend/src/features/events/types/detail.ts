import type { EventSaleStatus } from './event'

/** Forma de EventoDetalleResponse de msvc-evento (GET /api/eventos/{id}/detalle). */
export type EventoDetalleApi = {
  id: number
  nombre: string
  descripcion: string | null
  tipo: string
  fechaHora: string
  estado: string
  lugar: { nombre: string; direccion: string | null; ciudad: string | null }
  participantes: { id: number; nombre: string; tipo: string }[]
  sectores: {
    idSector: number
    nombre: string | null
    tipo: string | null
    capacidad: number | null
    filas: number | null
    asientosPorFila: number | null
    precio: number
  }[]
}

/** Forma de DisponibilidadResponse (GET /api/eventos/{id}/disponibilidad). */
export type DisponibilidadApi = {
  idEvento: number
  idSector: number
  capacidad: number
  ocupadas: number
  disponibles: number
}

export type SectorInfo = {
  id: number
  name: string
  price: number
  capacity: number | null
}

export type EventDetail = {
  id: number
  title: string
  description: string | null
  category: string
  startsAt: string
  status: EventSaleStatus
  venue: { name: string; address: string | null; city: string | null }
  lineup: { id: number; name: string; kind: string }[]
  sectors: SectorInfo[]
}

/** Entradas que todavía se pueden comprar en un sector. */
export type SectorAvailability = { available: number; capacity: number }
