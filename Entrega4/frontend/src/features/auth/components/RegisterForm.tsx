import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Checkbox } from '@/components/ui/Checkbox'
import { PasswordField } from '@/components/ui/PasswordField'
import { TextField } from '@/components/ui/TextField'
import { useRegister } from '../hooks/useRegister'
import { registerSchema, toRegisterRequest, type RegisterValues } from '../schemas/registerSchema'
import { describeRegisterError } from '../utils/authErrors'

const EMPTY: RegisterValues = { nombre: '', apellido: '', dni: '', ciudad: '', email: '', password: '', acceptTerms: false }

export const RegisterForm = () => {
  const navigate = useNavigate()
  const registration = useRegister()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterValues>({ resolver: zodResolver(registerSchema), defaultValues: EMPTY })

  const onSubmit = handleSubmit((values) =>
    registration.mutate(toRegisterRequest(values), { onSuccess: () => navigate('/') }),
  )
  const failure = registration.error ? describeRegisterError(registration.error) : null

  return (
    <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
      {failure && (
        <Alert tone="error" title={failure.title}>
          {failure.message}
        </Alert>
      )}
      <div className="grid grid-cols-2 gap-4">
        <TextField label="Nombre" placeholder="Valentina" autoComplete="given-name" error={errors.nombre?.message} {...register('nombre')} />
        <TextField label="Apellido" placeholder="Ruiz" autoComplete="family-name" error={errors.apellido?.message} {...register('apellido')} />
        <TextField label="DNI" inputMode="numeric" placeholder="30123456" help="Sin puntos ni espacios." error={errors.dni?.message} {...register('dni')} />
        <TextField label="Ciudad" icon="pin" placeholder="Buenos Aires" autoComplete="address-level2" error={errors.ciudad?.message} {...register('ciudad')} />
      </div>
      <TextField label="Correo electrónico" type="email" icon="mail" placeholder="nombre@correo.com" autoComplete="email" error={errors.email?.message} {...register('email')} />
      <PasswordField label="Contraseña" placeholder="Mínimo 8 caracteres" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
      <div className="flex flex-col gap-1.5">
        <Checkbox label="Acepto los términos y condiciones" invalid={Boolean(errors.acceptTerms)} {...register('acceptTerms')} />
        {errors.acceptTerms && <p className="text-[13px] leading-[18px] font-medium text-error-fg">{errors.acceptTerms.message}</p>}
      </div>
      <Button type="submit" size="lg" loading={registration.isPending} className="w-full">
        Crear cuenta
      </Button>
    </form>
  )
}
