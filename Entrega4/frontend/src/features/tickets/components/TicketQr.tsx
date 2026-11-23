import { QRCodeSVG } from 'qrcode.react'

/** Código QR de una entrada válida: es lo que lee el personal de acceso en la puerta. */
export const TicketQr = ({ value }: { value: string }) => (
  <div className="w-fit rounded-md bg-white p-2.5">
    <QRCodeSVG value={value} size={132} role="img" aria-label="Código QR de la entrada" />
  </div>
)
