import { cn } from '@/lib/cn'

/** Ilustración de círculos (luna y órbitas) que decora héroes y paneles de marca. */
export const SceneArt = ({ className }: { className?: string }) => (
  <svg viewBox="0 0 520 520" aria-hidden="true" className={cn('pointer-events-none absolute size-130', className)}>
    <circle cx="300" cy="260" r="236" className="fill-none stroke-mint/20 stroke-2" />
    <circle cx="300" cy="260" r="176" className="fill-none stroke-bone/15 stroke-2" />
    <circle cx="300" cy="260" r="112" className="fill-mint" />
    <circle cx="338" cy="226" r="104" className="fill-ink" />
  </svg>
)
