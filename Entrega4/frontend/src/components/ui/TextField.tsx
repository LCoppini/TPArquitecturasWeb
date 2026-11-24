import { useId } from 'react'
import { Field } from './Field'
import { Input, type InputProps } from './Input'

export type TextFieldProps = Omit<InputProps, 'invalid'> & {
  label: string
  help?: string
  error?: string
}

/** Campo completo de formulario: etiqueta, input y mensaje de ayuda o error enlazados por accesibilidad. */
export const TextField = ({ label, help, error, id, className, ...inputProps }: TextFieldProps) => {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  const describedBy = error ? `${fieldId}-error` : help ? `${fieldId}-help` : undefined

  return (
    <Field id={fieldId} label={label} help={help} error={error} className={className}>
      <Input id={fieldId} invalid={Boolean(error)} aria-describedby={describedBy} {...inputProps} />
    </Field>
  )
}
