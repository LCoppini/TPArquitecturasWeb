import { useQuery } from '@tanstack/react-query'
import { ticketsApi } from '../api/ticketsApi'

export const useMyTickets = () => useQuery({ queryKey: ['my-tickets'], queryFn: ({ signal }) => ticketsApi.list(signal) })
