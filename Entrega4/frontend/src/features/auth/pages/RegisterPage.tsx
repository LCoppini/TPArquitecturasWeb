import { AuthSplitLayout } from '@/components/layout/AuthSplitLayout'
import { AuthBrandPanel } from '../components/AuthBrandPanel'
import { AuthFormPanel } from '../components/AuthFormPanel'
import { AuthSwitchLink } from '../components/AuthSwitchLink'
import { RegisterForm } from '../components/RegisterForm'

export const RegisterPage = () => (
  <AuthSplitLayout aside={<AuthBrandPanel />}>
    <AuthFormPanel
      title="Creá tu cuenta"
      subtitle={<AuthSwitchLink question="¿Ya tenés cuenta?" to="/ingresar" label="Ingresá" />}
    >
      <RegisterForm />
    </AuthFormPanel>
  </AuthSplitLayout>
)
