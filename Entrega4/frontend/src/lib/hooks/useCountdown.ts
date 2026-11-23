import { useEffect, useState } from 'react'

/** Segundos que faltan para `targetIso` (0 al vencer). Se actualiza cada segundo. */
export const useCountdown = (targetIso: string | null | undefined): number | null => {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!targetIso) return undefined
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [targetIso])

  if (!targetIso) return null
  return Math.max(Math.ceil((new Date(targetIso).getTime() - now) / 1000), 0)
}
