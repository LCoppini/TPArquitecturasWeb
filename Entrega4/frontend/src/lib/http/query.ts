export type QueryParams = Record<string, string | number | boolean | undefined | null>

/** Arma el query string omitiendo valores vacíos. Devuelve '' si no queda ningún parámetro. */
export const buildQuery = (params: QueryParams = {}): string => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}
