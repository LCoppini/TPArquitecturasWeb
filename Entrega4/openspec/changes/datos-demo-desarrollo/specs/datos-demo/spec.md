# Especificación: datos de demostración

## Requisito: el seed solo corre con el perfil `dev-seed`

#### Escenario: sin perfil
- **Cuando** un servicio arranca sin el perfil `dev-seed`
- **Entonces** no se crea ningún dato de demostración

#### Escenario: con perfil
- **Cuando** msvc-usuarios, msvc-ubicacion o msvc-evento arrancan con `dev-seed`
- **Entonces** cada uno carga sus datos de demostración y lo informa en el log

## Requisito: el seed es idempotente

#### Escenario: usuarios existentes
- **Cuando** ya existe un usuario con un correo de demostración
- **Entonces** se omite ese usuario y se crean solo los que faltan

#### Escenario: lugares o eventos existentes
- **Cuando** la tabla de lugares o de eventos ya tiene registros
- **Entonces** el seed correspondiente no inserta nada

## Requisito: usuarios de demostración

El seed de msvc-usuarios crea, con la contraseña `seed.dev.password` (por defecto `Ticketya123!`):
`admin@ticketya.dev` (ADMINISTRADOR), `organizador@ticketya.dev` (ORGANIZADOR), `cliente@ticketya.dev` y `cliente2@ticketya.dev` (CLIENTE) y `acceso@ticketya.dev` (PERSONAL_ACCESO).

#### Escenario: ingreso con un usuario de demostración
- **Cuando** se ingresa con `cliente@ticketya.dev` y la contraseña de demostración
- **Entonces** el servicio entrega un token con el rol CLIENTE

## Requisito: cartelera con datos variados

El seed de msvc-evento crea eventos de las categorías Recital, Teatro, Deporte y Festival, la mayoría PUBLICADOS con fecha futura, más al menos uno AGOTADO, CANCELADO, BORRADOR y FINALIZADO.

#### Escenario: catálogo público
- **Cuando** se consulta `GET /api/eventos` sin filtros
- **Entonces** se listan solo los eventos PUBLICADOS con fecha futura, con precio por sector
