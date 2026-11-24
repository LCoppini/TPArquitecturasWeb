export type EventCategory = {
  /** Valor que viaja en la dirección de la página: /?categoria=recitales. */
  slug: string
  label: string
  /** Valor del campo `tipo` que usa el backend. A confirmar contra los datos cargados. */
  tipo: string
}

/** Única definición de categorías: la usan el filtro, la barra de navegación y las tarjetas. */
export const EVENT_CATEGORIES: EventCategory[] = [
  { slug: 'recitales', label: 'Recitales', tipo: 'RECITAL' },
  { slug: 'teatro', label: 'Teatro', tipo: 'TEATRO' },
  { slug: 'deportes', label: 'Deportes', tipo: 'DEPORTE' },
  { slug: 'festivales', label: 'Festivales', tipo: 'FESTIVAL' },
]

export const findCategory = (slug: string | null | undefined): EventCategory | undefined =>
  EVENT_CATEGORIES.find((category) => category.slug === slug)

/** Etiqueta legible de un `tipo` del backend; si no se conoce, lo muestra en formato de oración. */
export const categoryLabelOf = (tipo: string): string => {
  const known = EVENT_CATEGORIES.find((category) => category.tipo === tipo.toUpperCase())
  if (known) return known.label
  const lower = tipo.toLowerCase()
  return lower.charAt(0).toUpperCase() + lower.slice(1)
}
