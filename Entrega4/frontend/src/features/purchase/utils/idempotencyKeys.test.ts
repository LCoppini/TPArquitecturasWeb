import { createKeyBook } from './idempotencyKeys'

describe('createKeyBook', () => {
  it('devuelve la misma clave para la misma operación y otra si cambian los datos', () => {
    const keyFor = createKeyBook()
    expect(keyFor('1:2:3')).toBe(keyFor('1:2:3'))
    expect(keyFor('1:2:4')).not.toBe(keyFor('1:2:3'))
  })
})
