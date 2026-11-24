import { Component, type ErrorInfo, type ReactNode } from 'react'
import { ServerErrorPage } from '@/pages/ServerErrorPage'

type State = { failed: boolean }

/** Último recurso: si algo se rompe al dibujar la interfaz, muestra el error general en lugar de una pantalla en blanco. */
export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Error de interfaz', error, info.componentStack)
  }

  render() {
    if (this.state.failed) return <ServerErrorPage onRetry={() => this.setState({ failed: false })} />
    return this.props.children
  }
}
