import { useQuery } from '@tanstack/react-query'
import { notificationsApi } from '../api/notificationsApi'

export const useMyNotifications = () =>
  useQuery({ queryKey: ['my-notifications'], queryFn: ({ signal }) => notificationsApi.mine(signal) })
