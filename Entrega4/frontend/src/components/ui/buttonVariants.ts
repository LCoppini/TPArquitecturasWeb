import { cva } from 'class-variance-authority'

export const buttonVariants = cva(
  [
    'relative inline-flex items-center justify-center gap-2 rounded-pill border-[1.5px] border-transparent',
    'text-button whitespace-nowrap no-underline cursor-pointer motion-fast',
    'transition-[background-color,box-shadow,border-color,color]',
    'disabled:cursor-not-allowed disabled:border-line-subtle disabled:bg-surface-alt disabled:text-fg-disabled',
    'disabled:shadow-none disabled:no-underline',
  ],
  {
    variants: {
      variant: {
        primary:
          'bg-brand text-on-brand hover:bg-brand-hover hover:shadow-glow active:bg-brand-pressed active:shadow-none',
        secondary: 'bg-inverse text-fg-inverse hover:opacity-90 hover:shadow-md active:opacity-80 active:shadow-none',
        outline: 'border-line-strong bg-transparent text-fg hover:bg-surface-alt active:bg-line-subtle',
        text: 'rounded-md bg-transparent px-3 text-fg hover:bg-surface-alt hover:underline hover:underline-offset-4 disabled:border-transparent disabled:bg-transparent',
        icon: 'border-line bg-surface p-0 text-fg hover:border-line-strong hover:bg-surface-alt',
      },
      size: {
        sm: 'h-9 px-4 text-[14px]',
        md: 'h-12 px-6',
        lg: 'h-14 px-8 text-[16px]',
      },
    },
    compoundVariants: [
      { variant: 'icon', size: 'sm', className: 'w-9 px-0' },
      { variant: 'icon', size: 'md', className: 'w-12 px-0' },
      { variant: 'icon', size: 'lg', className: 'w-14 px-0' },
    ],
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)
