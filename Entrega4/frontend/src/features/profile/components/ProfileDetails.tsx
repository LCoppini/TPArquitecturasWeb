import { Avatar } from '@/components/ui/Avatar'
import { Badge } from '@/components/ui/Badge'
import type { UsuarioApi } from '../api/profileApi'

const ROLE_LABELS: Record<string, string> = {
  CLIENTE: 'Cliente',
  ORGANIZADOR: 'Organizador',
  ADMINISTRADOR: 'Administrador',
  PERSONAL_ACCESO: 'Personal de acceso',
}

const Row = ({ label, value }: { label: string; value: string }) => (
  <div className="flex flex-col gap-0.5 border-b border-line-subtle py-3 last:border-b-0">
    <dt className="text-caption text-fg-muted">{label}</dt>
    <dd className="text-body-m text-fg">{value}</dd>
  </div>
)

/** Datos personales de la cuenta, de solo lectura. */
export const ProfileDetails = ({ user }: { user: UsuarioApi }) => (
  <div className="flex flex-col gap-6 rounded-xl border border-line-subtle bg-surface p-6">
    <div className="flex items-center gap-4">
      <Avatar initials={`${user.nombre[0] ?? ''}${user.apellido[0] ?? ''}`.toUpperCase()} className="size-16" />
      <div className="flex flex-col gap-1.5">
        <h2 className="text-h3 text-fg">
          {user.nombre} {user.apellido}
        </h2>
        <div className="flex gap-2">
          {user.roles.map(({ idRol, tipo }) => (
            <Badge key={idRol} tone="info">
              {ROLE_LABELS[tipo] ?? tipo}
            </Badge>
          ))}
        </div>
      </div>
    </div>
    <dl>
      <Row label="Correo electrónico" value={user.email ?? 'Sin correo registrado'} />
      <Row label="DNI" value={String(user.dni)} />
      <Row label="Ciudad" value={user.ciudad} />
    </dl>
  </div>
)
