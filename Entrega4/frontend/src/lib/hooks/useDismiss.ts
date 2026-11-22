import { useEffect, type RefObject } from 'react'

/** Llama a `onDismiss` al hacer clic fuera del elemento o al presionar Escape (mientras `active`). */
export const useDismiss = (ref: RefObject<HTMLElement | null>, onDismiss: () => void, active: boolean) => {
  useEffect(() => {
    if (!active) return undefined
    const onPointer = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) onDismiss()
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onDismiss()
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [ref, onDismiss, active])
}
