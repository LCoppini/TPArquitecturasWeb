# User Stories y criterios de aceptación

## US-1: Como organizador quiero crear un evento en borrador para diseñar

### Criterios de aceptación

- Verificar que es usuario tenga rol de organizador.
- Verificar que el evento se cree en estado borrador.

## US-2: Como organizador quiero configurar sectores y precios del evento para definir como será la venta.

### Criterios de aceptación

- Verificar que el usuario tenga el rol de organizador.
- Verificar que se permita indicar si el sector es numerado o no numerado.
- Verificar que se pueda modificar los sectores y precio.
- Verificar que un sector numerado requiera filas y asientos, y uno no numerado una capacidad máxima.

## US-3: Como organizador quiero publicar un evento para habilitar la venta.

### Criterios de aceptación

- Verificar que el usuario tenga rol de organizador.
- Verificar que el evento tenga los sectores con precio definido antes de poder publicarse.
- Verificar que, al publicar, el estado cambie de "borrador" a "publicado".

## US-4 : Como admin quiero cancelar cualquier evento para intervenir cuando sea necesario.

### Criterios de aceptación

- Verificar que el evento este creado.
- Verficar que se pueda cambiar el estado de los eventos.
- Verificar que luego de cancelar un evento, las entradas pasen al estado "en reintegro".
- Verificar que luego de cancelar un evento se notifique a todos los compradores el estado del evento.

## US-5: Como cliente quiero elegir un evento para comprar una entrada

### Criterios de aceptación

- Verificar que se reserve temporalmente el lugar/sector del evento por 10 minutos mientras se procesa la compra.
- Verificar que si se excede el tiempo de reserva, se cancele la selección del lugar/sector.
- Verificar que haya disponibilidad de entradas/asientos para el evento.
- Verificar que se indique el sector.
- Verificar en caso de ser sector numerado, se elija asiento especifico.
- Verificar que no se supere la cantidad máxima de entradas permitidas por cliente para ese evento.

## US-6: Como cliente quiero realizar la solicitud de devolución de una entrada para que se me re-integre el dinero.

### Criterios de aceptación

- Verificar que la solicitud sea 72hs antes del la realizacion del evento.
- Verificar que la entrada este comprada.
- Verificar que la solicitud se registre en estado "pendiente de revision".

## US-7: Como personal de acceso quiero escanear el QR de las entradas para verificar su autenticidad.

### Criterios de aceptación

- ‌
- Verificar que la entrada exista.
- Verificar que la entrada corresponda al evento al que asiste.
- Verificar que la entrada no haya sido utilizada antes.
- Verificar que la entrada se marque como utilizada.

## US-8: Como admin quiero poder modificar el estado de una solicitud de devolución de una entrada.

### Criterios de aceptación

- Verificar que el rol del usuario de administrador.
- Verificar que exista una solicitud de devolución pendiente para esa entrada.
- Verificar que si se acepta la solicitud, el asiento sea liberado.
- Verificar que se solicite el reintegro del dinero a la paserela de pagos.
- Verificar en ambos casos (aceptar/rechazar) la solicitud, se notifique al cliente la resolusion.

## US-9: Como cliente quiero buscar eventos filtrando la búsqueda para encontrar eventos de interés.

### Criterios de aceptación

- Verificar que se pueda buscar por tipo de evento, organizador, participante, ciudad y rango de fechas, y que los resultados cumplan el criterio indicado.
- Verificar que, al combinar varios criterios, los resultados cumplan todos a la vez.
- Verificar que, dado un resultado de búsqueda, cuando ordeno por fecha, los eventos aparecen del más próximo al más lejano.
- Verificar que, dado el resultado de una búsqueda, se permita ordenar de menor a mayor precio y viceversa.
- Verificar que, dado un resultado de búsqueda, se permita ordenar por relevancia (de mayor a menor cantidad de entradas vendidas).
- Verificar que, en caso de no existir criterio de filtrado, los eventos se muestren en un orden por defecto (próximos).
- Verificar que los resultados de búsqueda, en caso de superar los 10 eventos, se devuelvan paginados.

## US-10:  Como cliente quiero proceder al pago para confirmar la asistencia al evento.

### Criterios de aceptación

- Verificar que si el pago es aprobado, se emita la entrada con código QR único.
- Verificar que si el pago es rechazado, se libere el lugar que se había seleccionado.
- Verificar que se registre el detalle de la compra y el medio de pago.
- Verificar que, ante múltiples intentos de compra simultáneos sobre el mismo asiento (o el último lugar disponible de un sector), solo uno resulte exitoso y el resto sea rechazado.
- Verificar que, si un cliente reintenta el pago de la misma compra por un
- problema de conexión, no se genere una doble emisión de entrada.

## US-11:  Como cliente quiero poder recibir notificaciones para  estar al tanto de lo eventos.

### Criterios de aceptación

- Verificar que se envíe una notificación al cliente cuando se confirma una compra.
- Verificar que se envíe una notificación cuando el evento comprado se cancela o modifica.
- Verificar que se envíe una notificación cuando un artista favorito del cliente publica un evento en su ciudad.

## US-12:  Como organizador quiero poder consultar reportes de mis eventos para poder administrarlos.

### Criterios de aceptación

- Verificar que se muestren las entradas vendidas y devueltas por sector.
- Verificar que se muestre el porcentaje de ocupación y recaudación.

## US-13: Como cliente quiero ingresar a una fila virtual para comprar en orden cuando el evento es de alta demanda.

### Criterios de aceptación

- Verificar que el evento este marcado como de alta demanda.
- Verificar que se abrio la venta  a los clientes.
- Verificar que se redirija a una fila virtual.
- Verificar que el sistema le informe su posicion y los habilite a comprar en orden.
- Verficar que las compras sean por tandas.
