import { cva, type VariantProps } from 'class-variance-authority'
import type { ReactNode } from 'react'
import { Icon } from './Icon'
import type { IconName } from './iconPaths'

const alertVariants = cva('flex gap-3.5 rounded-md border px-[18px] py-4 text-[15px] leading-[22px] text-fg', {
  variants: {
    tone: {
      error: 'border-error-border bg-error-bg [&_svg]:text-error-fg [&_[data-title]]:text-error-fg',
      success: 'border-success-border bg-success-bg [&_svg]:text-success-fg [&_[data-title]]:text-success-fg',
      warning: 'border-warning-border bg-warning-bg [&_svg]:text-warning-fg [&_[data-title]]:text-warning-fg',
      info: 'border-info-border bg-info-bg [&_svg]:text-info-fg [&_[data-title]]:text-info-fg',
    },
  },
  defaultVariants: { tone: 'info' },
})

type Tone = NonNullable<VariantProps<typeof alertVariants>['tone']>

const DEFAULT_ICONS: Record<Tone, IconName> = {
  error: 'alertCircle',
  success: 'checkCircle',
  warning: 'alertCircle',
  info: 'info',
}

type AlertProps = {
  tone?: Tone
  title: string
  children?: ReactNode
  className?: string
}

/** Mensaje destacado. Los errores se anuncian de inmediato (`role="alert"`); el resto, de forma cortés. */
export const Alert = ({ tone = 'info', title, children, className }: AlertProps) => (
  <div role={tone === 'error' ? 'alert' : 'status'} className={alertVariants({ tone, className })}>
    <Icon name={DEFAULT_ICONS[tone]} className="mt-px size-[22px]" />
    <div className="flex-1">
      <div data-title className="font-bold">
        {title}
      </div>
      {children}
    </div>
  </div>
)
