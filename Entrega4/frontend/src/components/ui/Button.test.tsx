import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button } from './Button'

describe('Button', () => {
  it('dispara onClick', async () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Comprar</Button>)
    await userEvent.click(screen.getByRole('button', { name: 'Comprar' }))
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('en carga se marca como ocupado y no dispara onClick', async () => {
    const onClick = vi.fn()
    render(<Button loading onClick={onClick}>Comprar</Button>)
    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('aria-disabled', 'true')
    await userEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('con asChild renderiza el elemento hijo', () => {
    render(<Button asChild><a href="/x">Ir</a></Button>)
    expect(screen.getByRole('link', { name: 'Ir' })).toHaveAttribute('href', '/x')
  })
})
