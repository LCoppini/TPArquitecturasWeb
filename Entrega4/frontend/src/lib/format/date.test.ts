import { formatEventDate, formatEventDay } from './date'

describe('formato de fechas de eventos', () => {
  // 2026-11-28 00:00 UTC = viernes 27 de noviembre, 21:00 en Argentina (UTC-3).
  const iso = '2026-11-28T00:00:00Z'

  it('muestra la hora de Argentina sin importar la zona del navegador', () => {
    expect(formatEventDate(iso)).toBe('Vie 27 nov · 21:00')
  })

  it('devuelve día y mes abreviado en mayúsculas', () => {
    expect(formatEventDay(iso)).toEqual({ day: '27', month: 'NOV' })
  })
})
