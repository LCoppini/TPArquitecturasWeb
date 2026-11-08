# Historias de usuario y criterios de aceptación

## US-1: Crear un evento en borrador

**Como organizador**, quiero crear un evento en borrador para diseñarlo y publicarlo luego.

### Criterios de aceptación

- Verificar que el usuario tenga el rol de organizador.
- Verificar que el evento se cree en estado `borrador`.

---

## US-2: Configurar sectores y precios del evento

**Como organizador**, quiero configurar sectores y precios del evento para definir cómo será la venta.

### Criterios de aceptación

- Verificar que el usuario tenga el rol de organizador.
- Verificar que se permita indicar si el sector es numerado o no numerado.
- Verificar que se puedan modificar los sectores y sus precios.
- Verificar que se pueda publicar el evento.
- Verificar que un sector numerado requiera filas y asientos, y que uno no numerado requiera una capacidad máxima.

---

## US-3: Publicar un evento

**Como organizador**, quiero publicar un evento para habilitar la venta.

### Criterios de aceptación

- Verificar que el usuario tenga el rol de organizador.
- Verificar que el evento tenga los sectores con precio definido antes de poder publicarse.
- Verificar que, al publicar, el estado cambie de `borrador` a `publicado`.

---

## US-4: Suspender o cancelar un evento

**Como administrador**, quiero suspender o cancelar cualquier evento para intervenir cuando sea necesario.

### Criterios de aceptación

- Verificar que el evento esté creado.
- Verificar que se pueda cambiar el estado de los eventos.
- Verificar que, luego de cancelar un evento, las entradas pasen al estado `en reintegro`.
- Verificar que, luego de cancelar un evento, se notifique a todos los compradores sobre el estado del evento.

---

## US-5: Elegir un evento para comprar una entrada

**Como cliente**, quiero elegir un evento para comprar una entrada.

### Criterios de aceptación

- Verificar que se reserve temporalmente el lugar o sector del evento por 10 minutos mientras se procesa la compra.
- Verificar que, si se excede el tiempo de reserva, se cancele la selección del lugar o sector.
- Verificar que haya disponibilidad de entradas o asientos para el evento.
- Verificar que se indique el sector.
- Verificar que, en caso de ser un sector numerado, se pueda elegir un asiento específico.
- Verificar que no se supere la cantidad máxima de entradas permitidas por cliente para ese evento.

---

## US-6: Solicitar la devolución de una entrada

**Como cliente**, quiero realizar la solicitud de devolución de una entrada para que se me reintegre el dinero.

### Criterios de aceptación

- Verificar que la solicitud se realice 72 horas antes de la realización del evento.
- Verificar que la entrada esté comprada.
- Verificar que la solicitud se registre en estado `pendiente de revisión`.

---

## US-7: Escanear el QR de las entradas

**Como personal de acceso**, quiero escanear el QR de las entradas para verificar su autenticidad.

### Criterios de aceptación

- Verificar que la entrada exista.
- Verificar que la entrada corresponda al evento al que asiste el cliente.
- Verificar que la entrada no haya sido utilizada anteriormente.
- Verificar que la entrada se marque como utilizada.

---

## US-8: Modificar el estado de una solicitud de devolución

**Como administrador**, quiero poder modificar el estado de una solicitud de devolución de una entrada.

### Criterios de aceptación

- Verificar que el usuario tenga el rol de administrador.
- Verificar si la entrada está comprada.
- Verificar que, si se acepta la solicitud, el asiento sea liberado.
- Verificar que se solicite el reintegro del dinero a la pasarela de pagos.
- Verificar que, tanto al aceptar como al rechazar la solicitud, se notifique al cliente la resolución.

---

## US-9: Buscar eventos

**Como cliente**, quiero buscar eventos por tipo, organizador, participante, ciudad y rango de fechas para encontrar eventos de interés.

### Criterios de aceptación

- Verificar que se pueda buscar por tipo de evento, organizador, participante, ciudad y rango de fechas, y que los resultados cumplan con el criterio indicado.
- Verificar que, al combinar varios criterios, los resultados cumplan todos los criterios a la vez.
- Verificar que, al filtrar por rango de fechas, los eventos aparezcan del más próximo al más lejano.
- Verificar que se permita ordenar los resultados de menor a mayor precio y viceversa.
- Verificar que se permita ordenar los resultados por relevancia (de mayor a menor cantidad de entradas vendidas). **Este criterio podría eliminarse, ya que no se solicita explícitamente.**
- Verificar que, en caso de no existir criterios de filtrado, los eventos se muestren en un orden por defecto (los próximos primero).
- Verificar que los resultados de búsqueda se devuelvan paginados cuando superen los 10 eventos.

---

## US-10: Proceder al pago

**Como cliente**, quiero proceder al pago para confirmar la asistencia al evento.

### Criterios de aceptación

- Verificar que, si el pago es aprobado, se emita la entrada con un código QR único.
- Verificar que, si el pago es rechazado, se libere el lugar que se había seleccionado.
- Verificar que se registre el detalle de la compra y el medio de pago.
- Verificar que, ante múltiples intentos de compra simultáneos sobre el mismo asiento (o el último lugar disponible de un sector), solo uno resulte exitoso y el resto sea rechazado.
- Verificar que, si un cliente reintenta el pago de la misma compra por un problema de conexión, no se genere una doble emisión de entrada.

---

## US-11: Recibir notificaciones

**Como cliente**, quiero poder recibir notificaciones para estar al tanto de los eventos.

### Criterios de aceptación

- Verificar que se envíe una notificación al cliente cuando se confirma una compra.
- Verificar que se envíe una notificación cuando el evento comprado se cancela o modifica.
- Verificar que se envíe una notificación cuando un artista favorito del cliente publica un evento en su ciudad.

---

## US-12: Consultar reportes de eventos

**Como organizador**, quiero poder consultar reportes de mis eventos para administrarlos.

### Criterios de aceptación

- Verificar que se muestren las entradas vendidas y devueltas por sector.
- Verificar que se muestre el porcentaje de ocupación y la recaudación.

---

## US-13: Ingresar a una fila virtual

**Como cliente**, quiero ingresar a una fila virtual para comprar en orden cuando el evento es de alta demanda.

### Criterios de aceptación

- Verificar que el evento esté marcado como de alta demanda.
- Verificar que se haya abierto la venta a los clientes.
- Verificar que se redirija al cliente a una fila virtual.
- Verificar que el sistema informe su posición y lo habilite a comprar en orden.
- Verificar que las compras se realicen por tandas.
