/** Forma de EventoResponse de msvc-evento, tal como llega por el gateway. */
export type EventoApi = {
  id: number
  idLugar: number
  idOrganizador: number
  nombre: string
  descripcion: string | null
  tipo: string
  fechaHora: string
  estado: string
  participantes: { id: number; nombre: string; tipo: string }[]
  sectores: { idSector: number; precio: number }[]
}

/** Forma de EventoPaginaResponse: la página es base 0. */
export type EventoPaginaApi = {
  contenido: EventoApi[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export type EventSaleStatus = 'available' | 'soldout' | 'cancelled' | 'suspended' | 'finished'

/** Modelo de vista de un evento en el catálogo. Solo contiene lo que el backend entrega hoy. */
export type EventSummary = {
  id: number
  title: string
  category: string
  /** Fecha y hora de inicio en ISO 8601. */
  startsAt: string
  /** Menor precio entre los sectores, o null si todavía no hay precios. */
  fromPrice: number | null
  status: EventSaleStatus
  /** Nombre de la sede. Se completará cuando el backend lo exponga a los clientes. */
  venue?: string
}

/** Página del catálogo con numeración base 1, como se muestra en pantalla. */
export type EventPage = {
  items: EventSummary[]
  page: number
  totalPages: number
  totalItems: number
}
