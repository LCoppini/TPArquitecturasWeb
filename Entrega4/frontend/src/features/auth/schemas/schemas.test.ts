import { loginSchema } from './loginSchema'
import { registerSchema, toRegisterRequest } from './registerSchema'

const validRegister = {
  nombre: 'Valentina',
  apellido: 'Ruiz',
  dni: '30123456',
  ciudad: 'Tandil',
  email: 'vale@correo.com',
  password: 'secreta123',
  acceptTerms: true,
}

describe('loginSchema', () => {
  it('pide correo y contraseña', () => {
    const result = loginSchema.safeParse({ email: '', password: '' })
    expect(result.success).toBe(false)
  })

  it('valida el formato del correo', () => {
    expect(loginSchema.safeParse({ email: 'sin-arroba', password: 'x' }).success).toBe(false)
    expect(loginSchema.safeParse({ email: 'a@b.com', password: 'x' }).success).toBe(true)
  })
})

describe('registerSchema', () => {
  it('acepta datos válidos y arma el cuerpo con DNI numérico', () => {
    const parsed = registerSchema.parse(validRegister)
    expect(toRegisterRequest(parsed)).toEqual({
      dni: 30123456,
      nombre: 'Valentina',
      apellido: 'Ruiz',
      ciudad: 'Tandil',
      email: 'vale@correo.com',
      password: 'secreta123',
    })
  })

  it.each([
    ['dni con puntos', { dni: '30.123.456' }],
    ['dni corto', { dni: '12345' }],
    ['contraseña corta', { password: '1234567' }],
    ['contraseña larga', { password: 'a'.repeat(73) }],
    ['sin aceptar términos', { acceptTerms: false }],
    ['nombre vacío', { nombre: '   ' }],
  ])('rechaza %s', (_name, override) => {
    expect(registerSchema.safeParse({ ...validRegister, ...override }).success).toBe(false)
  })
})
