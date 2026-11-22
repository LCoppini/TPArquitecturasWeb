import type { SVGProps } from 'react'
import { cn } from '@/lib/cn'
import { ICON_PATHS, type IconName } from './iconPaths'

type IconProps = Omit<SVGProps<SVGSVGElement>, 'name'> & { name: IconName }

/** Ícono decorativo: hereda `currentColor` y se oculta a lectores de pantalla. */
export const Icon = ({ name, className, ...props }: IconProps) => (
  <svg
    viewBox="0 0 24 24"
    aria-hidden="true"
    className={cn('size-5 flex-none fill-none stroke-current stroke-[1.75] [stroke-linecap:round] [stroke-linejoin:round]', className)}
    {...props}
  >
    {ICON_PATHS[name]}
  </svg>
)
