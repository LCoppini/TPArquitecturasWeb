import { cn } from '@/lib/cn'

/** Wordmark de texto: "Ticket" en serif y "Ya" en cursiva con el acento. No hay isotipo. */
export const Logo = ({ className }: { className?: string }) => (
  <span className={cn('inline-flex items-center gap-0.5 font-serif text-[26px] leading-none font-semibold tracking-[-0.02em] text-fg', className)}>
    Ticket<b className="font-semibold text-brand-text italic">Ya</b>
  </span>
)
