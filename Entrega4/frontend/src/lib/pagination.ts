export type PageItem = number | 'gap'

/** Páginas visibles de la paginación: siempre la primera, la última y las vecinas de la actual. */
export const getPageItems = (current: number, total: number, siblings = 1): PageItem[] => {
  if (total <= 1) return []
  const wanted = new Set([1, total, current])
  for (let offset = 1; offset <= siblings; offset += 1) {
    wanted.add(current - offset)
    wanted.add(current + offset)
  }
  const pages = [...wanted].filter((page) => page >= 1 && page <= total).sort((a, b) => a - b)
  return pages.flatMap((page, index): PageItem[] => {
    const previous = pages[index - 1]
    return previous !== undefined && page - previous > 1 ? ['gap', page] : [page]
  })
}
