/** Puente entre el cliente HTTP y la sesión: el AuthProvider registra cómo obtener el token y qué hacer ante un 401. */
type AuthHandlers = {
  getToken: () => string | null
  onUnauthorized: () => void
}

const NO_OP: AuthHandlers = { getToken: () => null, onUnauthorized: () => undefined }

let handlers: AuthHandlers = NO_OP

export const setAuthHandlers = (next: AuthHandlers) => {
  handlers = next
}

export const getAuthHandlers = () => handlers
