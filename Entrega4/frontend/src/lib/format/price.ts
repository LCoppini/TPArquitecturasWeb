const NUMBER_FORMAT = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 0 })

/** 48500 -> "$ 48.500" (punto de miles y espacio tras el símbolo). */
export const formatPrice = (amount: number): string => `$ ${NUMBER_FORMAT.format(amount)}`
