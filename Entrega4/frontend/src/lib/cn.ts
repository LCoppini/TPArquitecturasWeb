import { clsx, type ClassValue } from 'clsx'
import { extendTailwindMerge } from 'tailwind-merge'

/** Nombres propios del tema (ver styles/theme.css) para que twMerge resuelva bien los conflictos. */
const COLORS = [
  'background', 'foreground', 'card', 'card-foreground', 'popover', 'popover-foreground', 'primary',
  'primary-foreground', 'secondary', 'secondary-foreground', 'muted', 'muted-foreground', 'accent',
  'accent-foreground', 'destructive', 'border', 'input', 'ring', 'page', 'surface', 'surface-alt', 'field',
  'inverse', 'glass', 'fg', 'fg-secondary', 'fg-muted', 'fg-disabled', 'fg-inverse', 'ink', 'bone', 'ink-line',
  'gray-soft', 'brand', 'brand-hover', 'brand-pressed', 'brand-soft', 'brand-text', 'on-brand', 'mint',
  'line-subtle', 'line', 'line-control', 'line-strong', 'line-glass', 'overlay', 'success-fg', 'success-bg',
  'success-border', 'error-fg', 'error-bg', 'error-border', 'warning-fg', 'warning-bg', 'warning-border',
  'info-fg', 'info-bg', 'info-border',
]

const TEXT_STYLES = [
  'display', 'h1', 'h2', 'h3', 'h4', 'title-card', 'price', 'body-l', 'body-m', 'body-s', 'caption', 'overline',
  'button',
]

const twMerge = extendTailwindMerge({
  extend: {
    theme: { color: COLORS, radius: ['pill'], shadow: ['glow'] },
    classGroups: { 'font-size': [{ text: TEXT_STYLES }] },
  },
})

/** Une clases condicionales y resuelve los conflictos de Tailwind (la última gana). */
export const cn = (...inputs: ClassValue[]) => twMerge(clsx(inputs))
