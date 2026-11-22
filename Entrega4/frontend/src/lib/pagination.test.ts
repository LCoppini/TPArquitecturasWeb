import { getPageItems } from './pagination'

describe('getPageItems', () => {
  it('no devuelve nada si hay una sola página', () => {
    expect(getPageItems(1, 1)).toEqual([])
  })

  it('muestra todas las páginas cuando son pocas', () => {
    expect(getPageItems(2, 3)).toEqual([1, 2, 3])
  })

  it('agrega separadores entre extremos y vecinas', () => {
    expect(getPageItems(5, 10)).toEqual([1, 'gap', 4, 5, 6, 'gap', 10])
  })

  it('no deja separador cuando solo falta una página', () => {
    expect(getPageItems(3, 6)).toEqual([1, 2, 3, 4, 'gap', 6])
  })
})
