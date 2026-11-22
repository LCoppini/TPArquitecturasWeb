import { useState } from 'react'
import { Button } from './Button'
import { Icon } from './Icon'
import { TextField, type TextFieldProps } from './TextField'

/** Campo de contraseña con botón para mostrar u ocultar lo escrito. */
export const PasswordField = (props: Omit<TextFieldProps, 'type' | 'trailing' | 'icon'>) => {
  const [visible, setVisible] = useState(false)

  return (
    <TextField
      {...props}
      icon="lock"
      type={visible ? 'text' : 'password'}
      trailing={
        <Button
          variant="text"
          className="-mr-2 size-11 p-0"
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          onClick={() => setVisible((current) => !current)}
        >
          <Icon name={visible ? 'eyeOff' : 'eye'} />
        </Button>
      }
    />
  )
}
