import { formatClock } from '@/lib/format/duration'
import { useCountdown } from '@/lib/hooks/useCountdown'
import { cn } from '@/lib/cn'

type CountdownProps = {
  /** Instante de vencimiento en ISO 8601. */
  until: string | null
  label: string
}

/** Cuenta regresiva visible; en rojo durante el último minuto. */
export const Countdown = ({ until, label }: CountdownProps) => {
  const seconds = useCountdown(until)
  if (seconds === null) return null
  return (
    <p className="text-body-m text-fg-secondary">
      {label}{' '}
      <strong data-testid="countdown" className={cn('font-mono text-fg', seconds < 60 && 'text-error-fg')}>
        {formatClock(seconds)}
      </strong>
    </p>
  )
}
