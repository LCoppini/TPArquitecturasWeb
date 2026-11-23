import { entrada } from '@/test/fixtures'
import type { EntradaApi } from '../types'
import { groupByPurchase } from './ticketStatus'
import { countGroups, filterGroups, sectionOfGroup } from './ticketTabs'

const t = (overrides: Record<string, unknown>) => entrada(overrides) as EntradaApi
const NOW = new Date('2026-11-23T12:00:00Z').getTime()
const FUTURE = new Map([[1, '2026-12-05T00:00:00Z']])
const PAST = new Map([[1, '2026-10-01T00:00:00Z']])

describe('ticketTabs', () => {
  it('una compra con entradas válidas de un evento futuro es próxima', () => {
    expect(sectionOfGroup(groupByPurchase([t({})])[0], FUTURE, NOW)).toBe('upcoming')
  })

  it('una entrada válida de un evento que ya pasó es pasada', () => {
    expect(sectionOfGroup(groupByPurchase([t({})])[0], PAST, NOW)).toBe('past')
  })

  it('sin la fecha del evento se la considera próxima hasta saberla', () => {
    expect(sectionOfGroup(groupByPurchase([t({})])[0], new Map(), NOW)).toBe('upcoming')
  })

  it('usadas, vencidas y canceladas son pasadas', () => {
    for (const estado of ['USADA', 'VENCIDA', 'CANCELADA']) {
      expect(sectionOfGroup(groupByPurchase([t({ estado })])[0], FUTURE, NOW)).toBe('past')
    }
  })

  it('devueltas y en reintegro van a devueltas', () => {
    expect(sectionOfGroup(groupByPurchase([t({ estado: 'DEVUELTA' })])[0], FUTURE, NOW)).toBe('returned')
    expect(sectionOfGroup(groupByPurchase([t({ estado: 'EN_REINTEGRO' })])[0], FUTURE, NOW)).toBe('returned')
  })

  it('si a la compra le queda una entrada por usar sigue siendo próxima', () => {
    const group = groupByPurchase([t({ id: 1, estado: 'DEVUELTA' }), t({ id: 2 })])[0]
    expect(sectionOfGroup(group, FUTURE, NOW)).toBe('upcoming')
  })

  it('filtra y cuenta por pestaña', () => {
    const groups = groupByPurchase([t({ id: 1, idCompra: 5 }), t({ id: 2, idCompra: 6, estado: 'USADA' }), t({ id: 3, idCompra: 7, estado: 'DEVUELTA' })])
    expect(countGroups(groups, FUTURE, NOW)).toEqual({ all: 3, upcoming: 1, past: 1, returned: 1 })
    expect(filterGroups(groups, 'all', FUTURE, NOW)).toHaveLength(3)
    expect(filterGroups(groups, 'past', FUTURE, NOW).map(({ purchaseId }) => purchaseId)).toEqual([6])
  })
})
