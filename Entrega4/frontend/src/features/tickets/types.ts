/** Forma de EntradaResponse de msvc-entrada. */
export type EntradaApi = {
  id: number
  idEvento: number
  idUsuario: number
  idSector: number
  fila: number | null
  asiento: number | null
  precio: number
  codigoQr: string | null
  /** RESERVADA, VALIDA, USADA, VENCIDA, CANCELADA, EN_REINTEGRO o DEVUELTA. */
  estado: string
  expiraEn: string | null
  idCompra: number | null
  /** PENDIENTE, APROBADA o RECHAZADA; null si no hubo devolución. */
  estadoDevolucion: string | null
  /** DEVOLUCION o CANCELACION cuando la entrada dejó de ser válida. */
  motivoInvalidacion: string | null
}
