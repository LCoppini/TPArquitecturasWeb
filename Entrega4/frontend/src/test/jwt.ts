const encode = (value: object) => btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')

/** Arma un JWT de prueba (sin firma válida) con los claims del backend. */
export const makeToken = (claims: { idUsuario?: number; roles?: string[]; exp: number }) =>
  `${encode({ alg: 'HS256' })}.${encode({ idUsuario: 1, roles: ['CLIENTE'], ...claims })}.firma`

export const secondsFromNow = (seconds: number) => Math.floor(Date.now() / 1000) + seconds
