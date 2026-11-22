import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { findCategory } from '../constants/categories'

/** El filtro y la página viven en la dirección (?categoria=teatro&pagina=2): se pueden compartir y recargar. */
export const useCatalogParams = () => {
  const [params, setParams] = useSearchParams()

  const category = findCategory(params.get('categoria'))
  const parsedPage = Number.parseInt(params.get('pagina') ?? '', 10)
  const page = Number.isFinite(parsedPage) && parsedPage >= 1 ? parsedPage : 1

  const selectCategory = useCallback(
    (slug: string | null) => {
      const next = new URLSearchParams()
      if (slug) next.set('categoria', slug)
      setParams(next)
    },
    [setParams],
  )

  const selectPage = useCallback(
    (nextPage: number) => {
      const next = new URLSearchParams(params)
      if (nextPage <= 1) next.delete('pagina')
      else next.set('pagina', String(nextPage))
      setParams(next)
    },
    [params, setParams],
  )

  return { category, page, selectCategory, selectPage }
}
