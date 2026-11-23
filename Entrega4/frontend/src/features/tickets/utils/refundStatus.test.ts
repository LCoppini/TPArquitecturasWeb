import { entrada } from '@/test/fixtures'
import type { EntradaApi } from '../types'
import { refundStatusOf } from './refundStatus'
import { groupByPurchase } from './ticketStatus'

const status = (...tickets: Record<string, unknown>[]) => refundStatusOf(groupByPurchase(tickets.map((ticket) => entrada(ticket) as EntradaApi))[0])

describe('refundStatusOf', () => {
  it('sin devolución no hay estado', () => {
    expect(status({})).toBeNull()
  })

  it('una solicitud sin resolver o un reintegro en curso está pendiente', () => {
    expect(status({ estado: 'EN_REINTEGRO', estadoDevolucion: 'PENDIENTE' })).toEqual({ kind: 'pending', cancelled: false })
    expect(status({ estado: 'EN_REINTEGRO', estadoDevolucion: 'APROBADA' })).toEqual({ kind: 'pending', cancelled: false })
  })

  it('el reintegro por cancelación del evento se distingue de la devolución pedida', () => {
    expect(status({ estado: 'EN_REINTEGRO', motivoInvalidacion: 'CANCELACION' })).toEqual({ kind: 'pending', cancelled: true })
  })

  it('una devolución rechazada deja las entradas vigentes y lo informa', () => {
    expect(status({ estadoDevolucion: 'RECHAZADA' })).toEqual({ kind: 'rejected' })
  })

  it('una devolución aprobada suma lo reintegrado', () => {
    expect(status({ id: 1, estado: 'DEVUELTA', estadoDevolucion: 'APROBADA', precio: 18000 }, { id: 2, estado: 'DEVUELTA', estadoDevolucion: 'APROBADA', precio: 18000 })).toEqual({
      kind: 'approved',
      amount: 36000,
      cancelled: false,
    })
  })
})
