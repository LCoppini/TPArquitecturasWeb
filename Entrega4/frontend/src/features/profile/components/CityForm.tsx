import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { ApiError } from '@/lib/http'
import { useUpdateCity } from '../hooks/useUpdateCity'
import { citySchema, type CityValues } from '../schemas/citySchema'

/** Cambio de ciudad: es lo único del perfil que la persona puede editar por su cuenta. */
export const CityForm = ({ currentCity }: { currentCity: string }) => {
  const update = useUpdateCity()
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<CityValues>({ resolver: zodResolver(citySchema), values: { ciudad: currentCity } })
  const unchanged = (useWatch({ control, name: 'ciudad' }) ?? '').trim() === currentCity

  return (
    <form
      noValidate
      onSubmit={handleSubmit(({ ciudad }) => update.mutate(ciudad))}
      className="flex flex-col gap-4 rounded-xl border border-line-subtle bg-surface p-6"
    >
      <h2 className="text-h3 text-fg">Tu ciudad</h2>
      {update.isSuccess && <Alert tone="success" title="Guardamos tu ciudad." />}
      {update.error && (
        <Alert tone="error" title="No pudimos guardar tu ciudad">
          {update.error instanceof ApiError ? update.error.message : 'Intentá de nuevo en unos minutos.'}
        </Alert>
      )}
      <TextField
        label="Ciudad"
        icon="pin"
        autoComplete="address-level2"
        help="La usamos para mostrarte primero los eventos cerca tuyo."
        error={errors.ciudad?.message}
        {...register('ciudad')}
      />
      <Button type="submit" loading={update.isPending} disabled={unchanged} className="self-start">
        Guardar cambios
      </Button>
    </form>
  )
}
