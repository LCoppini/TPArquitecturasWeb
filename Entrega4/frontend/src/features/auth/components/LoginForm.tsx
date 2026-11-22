import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { PasswordField } from '@/components/ui/PasswordField'
import { TextField } from '@/components/ui/TextField'
import { useLogin } from '../hooks/useLogin'
import { loginSchema, type LoginValues } from '../schemas/loginSchema'
import { describeLoginError } from '../utils/authErrors'

export const LoginForm = () => {
  const navigate = useNavigate()
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })

  const onSubmit = handleSubmit((values) => login.mutate(values, { onSuccess: () => navigate('/') }))
  const failure = login.error ? describeLoginError(login.error) : null

  return (
    <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
      {failure && (
        <Alert tone="error" title={failure.title}>
          {failure.message}
        </Alert>
      )}
      <TextField
        label="Correo electrónico"
        type="email"
        icon="mail"
        placeholder="nombre@correo.com"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <PasswordField
        label="Contraseña"
        placeholder="Tu contraseña"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" size="lg" loading={login.isPending} className="w-full">
        Ingresar
      </Button>
    </form>
  )
}
