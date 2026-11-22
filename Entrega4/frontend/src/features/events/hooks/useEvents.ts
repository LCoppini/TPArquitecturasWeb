import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { eventsApi } from '../api/eventsApi'
import type { EventCategory } from '../constants/categories'

export const EVENTS_PAGE_SIZE = 8

type UseEventsParams = {
  category?: EventCategory
  page: number
}

/** Consulta una página del catálogo. Mientras llega la siguiente, conserva la anterior para evitar parpadeos. */
export const useEvents = ({ category, page }: UseEventsParams) =>
  useQuery({
    queryKey: ['events', { tipo: category?.tipo ?? null, page }],
    queryFn: ({ signal }) => eventsApi.list({ tipo: category?.tipo, page, size: EVENTS_PAGE_SIZE, signal }),
    placeholderData: keepPreviousData,
  })
