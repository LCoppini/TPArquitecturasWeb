import { EVENT_CATEGORIES } from '@/features/events/constants/categories'
import type { FooterColumnData } from '@/components/layout/FooterLinkColumn'

/** Enlaces de categoría: llevan al inicio con el filtro aplicado. */
export const CATEGORY_LINKS = EVENT_CATEGORIES.filter(({ slug }) => slug !== 'festivales').map(({ slug, label }) => ({
  label,
  to: `/?categoria=${slug}`,
  slug,
}))

export const FOOTER_COLUMNS: FooterColumnData[] = [
  { title: 'Explorar', links: [{ label: 'Todos los eventos', to: '/' }, ...CATEGORY_LINKS.map(({ label, to }) => ({ label, to }))] },
  {
    title: 'Cuenta',
    links: [
      { label: 'Ingresar', to: '/ingresar' },
      { label: 'Crear cuenta', to: '/registro' },
    ],
  },
]
