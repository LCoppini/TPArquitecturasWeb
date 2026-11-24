import { screen } from '@testing-library/react'
import { renderWithApp } from '@/test/render'
import type { EventSummary } from '../types/event'
import { EventCard } from './EventCard'

const event: EventSummary = {
  id: 1,
  title: 'Noche de rock',
  category: 'Recitales',
  startsAt: '2026-11-28T00:00:00Z',
  fromPrice: 48500,
  status: 'available',
}

describe('EventCard', () => {
  it('muestra título, categoría, fecha, estado y precio', () => {
    renderWithApp(<EventCard event={event} />)
    expect(screen.getByRole('heading', { name: 'Noche de rock' })).toBeInTheDocument()
    expect(screen.getByText('Recitales')).toBeInTheDocument()
    expect(screen.getByText('Vie 27 nov · 21:00')).toBeInTheDocument()
    expect(screen.getByText('Disponible')).toBeInTheDocument()
    expect(screen.getByText(/48\.500/)).toBeInTheDocument()
  })

  it('no muestra sede si el backend no la entrega', () => {
    renderWithApp(<EventCard event={event} />)
    expect(screen.queryByText(/Estadio|Teatro/)).not.toBeInTheDocument()
  })

  it('enlaza el título solo cuando hay destino', () => {
    const { unmount } = renderWithApp(<EventCard event={event} />)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    unmount()
    renderWithApp(<EventCard event={event} href="/eventos/1" />)
    expect(screen.getByRole('link', { name: 'Noche de rock' })).toHaveAttribute('href', '/eventos/1')
  })

  it('un evento agotado muestra el estado y no el precio', () => {
    renderWithApp(<EventCard event={{ ...event, status: 'soldout' }} />)
    expect(screen.getByText('Agotado')).toBeInTheDocument()
    expect(screen.queryByText(/48\.500/)).not.toBeInTheDocument()
  })
})
