import { EVENT_CATEGORIES } from '@/features/events/constants/categories'
import type { FooterColumnData } from '@/components/layout/FooterLinkColumn'

/** Enlaces de categoría: llevan al inicio con el filtro aplicado. */
export const CATEGORY_LINKS = EVENT_CATEGORIES.filter(({ slug }) => slug !== 'festivales').map(({ slug, label }) => ({
  label,
  to: `/?categoria=${slug}`,
  slug,
}))

const EXPLORE_COLUMN: FooterColumnData = {
  title: 'Explorar',
  links: [{ label: 'Todos los eventos', to: '/' }, ...CATEGORY_LINKS.map(({ label, to }) => ({ label, to }))],
}

const ACCOUNT_COLUMN: FooterColumnData = {
  title: 'Cuenta',
  links: [
    { label: 'Ingresar', to: '/ingresar' },
    { label: 'Crear cuenta', to: '/registro' },
  ],
}

/** Con sesión iniciada no se ofrecen los enlaces de ingreso y registro. */
export const footerColumns = (isAuthenticated: boolean): FooterColumnData[] =>
  isAuthenticated ? [EXPLORE_COLUMN] : [EXPLORE_COLUMN, ACCOUNT_COLUMN]
