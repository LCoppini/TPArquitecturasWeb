import { Link } from 'react-router-dom'

export type FooterColumnData = {
  title: string
  links: { label: string; to: string }[]
}

export const FooterLinkColumn = ({ title, links }: FooterColumnData) => (
  <nav aria-label={title}>
    <h3 className="mb-4 text-overline text-gray-soft">{title}</h3>
    <ul className="flex flex-col gap-2.5">
      {links.map(({ label, to }) => (
        <li key={label}>
          <Link to={to} className="text-[15px] leading-[22px] text-bone no-underline hover:text-mint hover:underline hover:underline-offset-4">
            {label}
          </Link>
        </li>
      ))}
    </ul>
  </nav>
)
