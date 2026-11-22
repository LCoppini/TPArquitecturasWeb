import { Link } from 'react-router-dom'
import { Logo } from '@/components/ui/Logo'
import { FooterLinkColumn, type FooterColumnData } from './FooterLinkColumn'

/** Pie de página oscuro: marca, columnas de enlaces y aclaración de precios. */
export const Footer = ({ columns }: { columns: FooterColumnData[] }) => (
  <footer data-theme="dark" className="flex flex-col gap-10 rounded-2xl bg-ink px-14 pt-14 pb-8 text-bone">
    <div className="grid grid-cols-[1.6fr_1fr_1fr] gap-8">
      <div>
        <Link to="/" aria-label="TicketYa, ir al inicio">
          <Logo className="text-[30px]" />
        </Link>
        <p className="mt-4 max-w-75 text-body-s text-gray-soft">
          Entradas para recitales, teatro y deportes. Comprá en pocos pasos y recibilas al instante.
        </p>
      </div>
      {columns.map((column) => (
        <FooterLinkColumn key={column.title} {...column} />
      ))}
    </div>
    <div className="flex items-center justify-between border-t border-ink-line pt-6 text-[13px] leading-[18px] text-gray-soft">
      <span>© 2026 TicketYa. Todos los derechos reservados.</span>
      <span>Precios en pesos argentinos, con cargos por servicio incluidos.</span>
    </div>
  </footer>
)
