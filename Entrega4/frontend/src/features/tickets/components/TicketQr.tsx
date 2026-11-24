import { QRCodeSVG } from 'qrcode.react'

/** Código QR de una entrada válida: es lo que lee el personal de acceso en la puerta. */
export const TicketQr = ({ value }: { value: string }) => (
  <div className="w-fit flex-none rounded-md border border-line-subtle bg-white p-2">
    <QRCodeSVG value={value} size={88} role="img" aria-label="Código QR de la entrada" />
  </div>
)
