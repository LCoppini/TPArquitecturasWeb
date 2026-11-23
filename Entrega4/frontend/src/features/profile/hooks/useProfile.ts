import { useQuery } from '@tanstack/react-query'
import { profileApi } from '../api/profileApi'

export const useProfile = () => useQuery({ queryKey: ['profile'], queryFn: ({ signal }) => profileApi.me(signal) })
