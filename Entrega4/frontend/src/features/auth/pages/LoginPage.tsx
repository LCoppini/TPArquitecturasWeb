import { AuthSplitLayout } from '@/components/layout/AuthSplitLayout'
import { AuthBrandPanel } from '../components/AuthBrandPanel'
import { AuthFormPanel } from '../components/AuthFormPanel'
import { AuthSwitchLink } from '../components/AuthSwitchLink'
import { LoginForm } from '../components/LoginForm'

export const LoginPage = () => (
  <AuthSplitLayout aside={<AuthBrandPanel />}>
    <AuthFormPanel
      title="Ingresá a tu cuenta"
      subtitle={<AuthSwitchLink question="¿Todavía no tenés cuenta?" to="/registro" label="Creá una" />}
    >
      <LoginForm />
    </AuthFormPanel>
  </AuthSplitLayout>
)
