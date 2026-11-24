export type Role = 'CLIENTE' | 'ORGANIZADOR' | 'ADMINISTRADOR' | 'PERSONAL_ACCESO'

export type LoginCredentials = {
  email: string
  password: string
}

/** Respuesta de POST /api/usuarios/login. */
export type LoginResponse = {
  token: string
  tipo: string
  expiraEnSegundos: number
}

/** Cuerpo de POST /api/usuarios/registro (ver RegistroRequest en msvc-usuarios). */
export type RegisterRequest = {
  dni: number
  nombre: string
  apellido: string
  ciudad: string
  email: string
  password: string
}

/** Datos de la persona que ingresó, derivados del token y del correo con el que se autenticó. */
export type Session = {
  token: string
  email: string
  idUsuario: number
  roles: Role[]
  /** Instante de vencimiento del token, en milisegundos desde epoch. */
  expiresAt: number
}
