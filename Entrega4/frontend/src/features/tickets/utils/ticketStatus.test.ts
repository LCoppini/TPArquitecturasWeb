import { entrada } from '@/test/fixtures'
import type { EntradaApi } from '../types'
import { canRefund, groupByPurchase, statusOf } from './ticketStatus'

const t = (overrides: Record<string, unknown>) => entrada(overrides) as EntradaApi

describe('ticketStatus', () => {
  it('agrupa por compra conservando el orden y deja las entradas sin compra solas', () => {
    const groups = groupByPurchase([t({ id: 1, idCompra: 5 }), t({ id: 2, idCompra: null }), t({ id: 3, idCompra: 5 })])
    expect(groups.map(({ key, tickets }) => [key, tickets.length])).toEqual([['compra-5', 2], ['solo-2', 1]])
  })

  it('solo permite devolver compras con entradas válidas y sin devolución previa', () => {
    expect(canRefund(groupByPurchase([t({})])[0])).toBe(true)
    expect(canRefund(groupByPurchase([t({ estado: 'USADA' })])[0])).toBe(false)
    expect(canRefund(groupByPurchase([t({ estadoDevolucion: 'PENDIENTE' })])[0])).toBe(false)
    expect(canRefund(groupByPurchase([t({ idCompra: null })])[0])).toBe(false)
  })

  it('traduce los estados y deja pasar los desconocidos', () => {
    expect(statusOf('EN_REINTEGRO').label).toBe('Devolución en curso')
    expect(statusOf('RARO').label).toBe('RARO')
  })
})
