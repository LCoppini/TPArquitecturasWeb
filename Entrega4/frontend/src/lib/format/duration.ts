/** 75 -> "2 min" (siempre se redondea hacia arriba); 5400 -> "1 h 30 min". Menos de un minuto: "menos de 1 min". */
export const formatDuration = (seconds: number): string => {
  if (seconds <= 0) return 'menos de 1 min'
  const totalMinutes = Math.max(Math.ceil(seconds / 60), 1)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours === 0) return `${minutes} min`
  return minutes === 0 ? `${hours} h` : `${hours} h ${minutes} min`
}

/** 125 -> "02:05". Para cuentas regresivas. */
export const formatClock = (seconds: number): string => {
  const safe = Math.max(Math.floor(seconds), 0)
  const mm = String(Math.floor(safe / 60)).padStart(2, '0')
  const ss = String(safe % 60).padStart(2, '0')
  return `${mm}:${ss}`
}
