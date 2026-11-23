const LABELS: Record<string, string> = {
  FALLBACK_CIUDAD: 'Eventos en tu ciudad',
}

const TECHNICAL_CODE = /^[A-Z0-9_]+$/

/** Traduce el código técnico del backend; el texto ya legible se muestra tal cual y un código sin traducción se oculta. */
export const reasonLabel = (reason: string): string | null => {
  if (LABELS[reason]) return LABELS[reason]
  return TECHNICAL_CODE.test(reason) ? null : reason
}
