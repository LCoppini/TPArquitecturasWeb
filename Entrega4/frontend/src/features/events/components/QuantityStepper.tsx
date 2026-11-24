import { Button } from '@/components/ui/Button'

type QuantityStepperProps = {
  value: number
  max: number
  onChange: (value: number) => void
}

/** Cantidad de entradas: de 1 hasta el máximo permitido por compra y por disponibilidad. */
export const QuantityStepper = ({ value, max, onChange }: QuantityStepperProps) => (
  <div className="flex items-center justify-between">
    <span id="quantity-label" className="text-[14px] font-medium text-fg">
      Cantidad de entradas
    </span>
    <div role="group" aria-labelledby="quantity-label" className="flex items-center gap-3">
      <Button variant="icon" size="sm" aria-label="Quitar una entrada" disabled={value <= 1} onClick={() => onChange(value - 1)}>
        −
      </Button>
      <output aria-live="polite" className="w-6 text-center text-[18px] font-semibold text-fg">
        {value}
      </output>
      <Button variant="icon" size="sm" aria-label="Agregar una entrada" disabled={value >= max} onClick={() => onChange(value + 1)}>
        +
      </Button>
    </div>
  </div>
)
