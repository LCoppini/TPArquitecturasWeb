export const eventoDetalle = (overrides: Record<string, unknown> = {}) => ({
  id: 1,
  nombre: 'Noche de Rock Nacional',
  descripcion: 'Una noche con los clásicos.',
  tipo: 'RECITAL',
  fechaHora: '2027-01-10T23:00:00Z',
  estado: 'PUBLICADO',
  lugar: { nombre: 'Estadio Central', direccion: 'Av. Siempre Viva 123', ciudad: 'Buenos Aires' },
  participantes: [{ id: 1, nombre: 'Los Fugitivos', tipo: 'ARTISTA' }],
  sectores: [
    { idSector: 1, nombre: 'Campo', tipo: 'NO_NUMERADO', capacidad: 4000, filas: null, asientosPorFila: null, precio: 32000 },
    { idSector: 2, nombre: 'Platea Baja', tipo: 'NUMERADO', capacidad: 600, filas: 20, asientosPorFila: 30, precio: 48000 },
  ],
  ...overrides,
})

export const disponibilidad = (idSector: number, disponibles: number) => ({
  idEvento: 1,
  idSector,
  capacidad: 100,
  ocupadas: 100 - disponibles,
  disponibles,
})

export const ingreso = (overrides: Record<string, unknown> = {}) => ({
  idEvento: 1,
  idIngreso: 7,
  secuencia: 3,
  estado: 'EN_ESPERA',
  posicion: 4,
  venceEn: null,
  idTanda: null,
  esperaEstimadaSegundos: 900,
  ...overrides,
})

export const entrada = (overrides: Record<string, unknown> = {}) => ({
  id: 11,
  idEvento: 1,
  idUsuario: 1,
  idSector: 2,
  fila: 1,
  asiento: 2,
  precio: 48000,
  codigoQr: 'qr-uuid-11',
  estado: 'VALIDA',
  expiraEn: null,
  idCompra: 5,
  estadoDevolucion: null,
  motivoInvalidacion: null,
  ...overrides,
})
