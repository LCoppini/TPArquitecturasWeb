import { formatPrice } from './price'

describe('formatPrice', () => {
  it('usa el formato argentino con separador de miles', () => {
    expect(formatPrice(48500)).toMatch(/^\$\s48\.500$/)
  })

  it('no muestra decimales', () => {
    expect(formatPrice(1200)).toMatch(/^\$\s1\.200$/)
  })
})
