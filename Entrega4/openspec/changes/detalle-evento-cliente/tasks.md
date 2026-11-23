## 1. Implementación
- [x] 1.1 DTOs `EventoDetalleResponse`, `LugarDetalleResponse`, `SectorDetalleResponse`
- [x] 1.2 Ampliar `LugarResponse` y `SectorResponse` con campos opcionales y constructores previos
- [x] 1.3 `EventoMapper.aDetalle`, `EventoService.obtenerDetalle` y `GET /api/eventos/{id}/detalle`

## 2. Pruebas
- [x] 2.1 `EventoDetalleServiceImplTest` (sectores, estados, sector desconocido, 404, ubicación caída)
- [ ] 2.2 Correr `mvn -pl msvc-evento test` y verificar que no falle ningún test previo
