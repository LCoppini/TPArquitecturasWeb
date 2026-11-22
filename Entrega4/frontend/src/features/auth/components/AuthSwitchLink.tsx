import { Link } from 'react-router-dom'

type AuthSwitchLinkProps = { question: string; to: string; label: string }

/** "¿Todavía no tenés cuenta? Creá una": enlace entre ingreso y registro. */
export const AuthSwitchLink = ({ question, to, label }: AuthSwitchLinkProps) => (
  <>
    {question}{' '}
    <Link to={to} className="font-semibold text-brand-text underline underline-offset-4">
      {label}
    </Link>
  </>
)
