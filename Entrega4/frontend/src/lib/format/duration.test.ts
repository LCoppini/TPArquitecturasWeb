import { formatClock, formatDuration } from './duration'

describe('duration', () => {
  it('redondea hacia arriba a minutos y componen horas', () => {
    expect(formatDuration(0)).toBe('menos de 1 min')
    expect(formatDuration(75)).toBe('2 min')
    expect(formatDuration(3600)).toBe('1 h')
    expect(formatDuration(5400)).toBe('1 h 30 min')
  })

  it('formatea la cuenta regresiva y no baja de cero', () => {
    expect(formatClock(125)).toBe('02:05')
    expect(formatClock(-4)).toBe('00:00')
  })
})
