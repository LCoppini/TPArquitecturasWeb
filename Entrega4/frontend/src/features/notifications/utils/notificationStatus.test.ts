import type { NotificacionApi } from '../types'
import { badgeOf, countNotifications, filterNotifications, groupOf, typeLabel } from './notificationStatus'

const n = (id: number, estado: string): NotificacionApi => ({ id, tipo: 'COMPRA_CONFIRMADA', asunto: null, estado, fechaCreacion: '2026-11-22T21:53:00Z', fechaEnvio: null })

describe('notificationStatus', () => {
  it('agrupa los estados según si el aviso llegó, está en camino o no se pudo enviar', () => {
    expect(groupOf('ENVIADA')).toBe('sent')
    expect(groupOf('PENDIENTE')).toBe('pending')
    expect(groupOf('ENVIANDO')).toBe('pending')
    expect(groupOf('FALLIDA')).toBe('failed')
    expect(groupOf('DESCARTADA')).toBe('failed')
  })

  it('filtra y cuenta por grupo', () => {
    const items = [n(1, 'ENVIADA'), n(2, 'ENVIADA'), n(3, 'PENDIENTE'), n(4, 'FALLIDA')]
    expect(countNotifications(items)).toEqual({ all: 4, sent: 2, pending: 1, failed: 1 })
    expect(filterNotifications(items, 'failed').map(({ id }) => id)).toEqual([4])
    expect(filterNotifications(items, 'all')).toHaveLength(4)
  })

  it('traduce tipos y estados, y deja pasar los desconocidos', () => {
    expect(typeLabel('EVENTO_MODIFICADO')).toBe('Evento modificado')
    expect(typeLabel('NUEVO_TIPO')).toBe('NUEVO_TIPO')
    expect(badgeOf('ENVIADA')).toEqual({ label: 'Enviada', tone: 'success' })
  })
})
