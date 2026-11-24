import { BrowserRouter } from 'react-router-dom'
import { AppErrorBoundary } from './components/feedback/AppErrorBoundary'
import { AppProviders } from './app/AppProviders'
import { AppRoutes } from './app/AppRoutes'

export const App = () => (
  <BrowserRouter>
    <AppProviders>
      <AppErrorBoundary>
        <AppRoutes />
      </AppErrorBoundary>
    </AppProviders>
  </BrowserRouter>
)
