import { reasonLabel } from './reasonLabel'

describe('reasonLabel', () => {
  it('traduce el código conocido', () => {
    expect(reasonLabel('FALLBACK_CIUDAD')).toBe('Eventos en tu ciudad')
  })
  it('oculta códigos técnicos sin traducción', () => {
    expect(reasonLabel('OTRO_CODIGO')).toBeNull()
  })
  it('deja pasar el texto ya legible', () => {
    expect(reasonLabel('Porque te gusta Los Fugitivos')).toBe('Porque te gusta Los Fugitivos')
  })
})
