package com.ticketya.entrada.config;

import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Datos de demostracion de msvc-entrada, solo con el perfil {@code dev-seed} y solo si no hay compras ni filas.
 *
 * <p>Deja una fila virtual ACTIVA en los 18 eventos publicados del seed de msvc-evento (ids 1 a 18 sobre una base
 * vacia) para poder comprar, y dos compras PAGADAS del cliente {@code seed.dev.id-cliente} (3 si msvc-usuarios
 * cargo su seed sobre una base vacia) para ver "Mis entradas" con codigo QR. Referencia eventos y sectores por id,
 * sin consultar a los otros servicios.
 */
@Component
@Profile("dev-seed")
public class DatosDemoEntradaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoEntradaSeeder.class);

    /** Capacidad total de cada evento publicado (ids 1 a 18): la suma de los sectores de su lugar. */
    static final List<Integer> CAPACIDAD_POR_EVENTO = List.of(
            5400, 3500, 5400, 1000, 1000, 1950, 1000, 3500, 1950,
            1000, 5400, 3500, 5400, 3500, 1950, 1000, 3500, 1950);

    private final FilaEventoRepository filaEventoRepository;
    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final PagoRepository pagoRepository;
    private final RegistroPagoRepository registroPagoRepository;
    private final long idCliente;

    public DatosDemoEntradaSeeder(
            FilaEventoRepository filaEventoRepository,
            CompraRepository compraRepository,
            EntradaRepository entradaRepository,
            PagoRepository pagoRepository,
            RegistroPagoRepository registroPagoRepository,
            @Value("${seed.dev.id-cliente:3}") long idCliente
    ) {
        this.filaEventoRepository = filaEventoRepository;
        this.compraRepository = compraRepository;
        this.entradaRepository = entradaRepository;
        this.pagoRepository = pagoRepository;
        this.registroPagoRepository = registroPagoRepository;
        this.idCliente = idCliente;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (filaEventoRepository.count() > 0 || compraRepository.count() > 0) {
            log.info("Seed de entrada omitido: ya hay filas o compras.");
            return;
        }
        for (int i = 0; i < CAPACIDAD_POR_EVENTO.size(); i++) {
            filaEventoRepository.save(fila(i + 1L, CAPACIDAD_POR_EVENTO.get(i)));
        }
        // Evento 1: dos platea baja numeradas. Evento 3: una entrada de campo (sector 1 es general en el lugar 1).
        compraPagada(1L, "seed-compra-1", 2L, new BigDecimal("48000.00"), List.of(new int[]{1, 1}, new int[]{1, 2}));
        compraPagada(3L, "seed-compra-2", 1L, new BigDecimal("18000.00"), List.<int[]>of(new int[]{0, 0}));
        log.info("Seed de entrada: {} filas y 2 compras para el usuario {}", CAPACIDAD_POR_EVENTO.size(), idCliente);
    }

    private FilaEvento fila(Long idEvento, int capacidad) {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(idEvento);
        fila.setEstado("ACTIVA");
        fila.setMaxPersonas(5);
        fila.setCupoEntradas(10);
        fila.setCapacidadTotal(capacidad);
        fila.setSiguienteSecuencia(1L);
        return fila;
    }

    /** Cada item es {fila, asiento}; {0, 0} significa sector sin numerar. */
    private void compraPagada(Long idEvento, String clave, Long idSector, BigDecimal precioUnitario, List<int[]> items) {
        Instant ahora = Instant.now();
        Compra compra = new Compra();
        compra.setIdUsuario(idCliente);
        compra.setIdEvento(idEvento);
        compra.setEstado(Compra.PAGADA);
        compra.setCreadaEn(ahora);
        compra.setVenceEn(ahora);
        compra.setCantidadEntradas(items.size());
        compra.setTotal(precioUnitario.multiply(BigDecimal.valueOf(items.size())));
        compra.setClaveIdempotencia(clave);
        compra.setHuellaSolicitud("seed");
        compra = compraRepository.save(compra);

        Pago pago = new Pago();
        pago.setIdUsuario(idCliente);
        pago.setIdCompra(compra.getIdCompra());
        pago.setEstado("APROBADO");
        pago.setMedioDePago("tarjeta");
        pago.setClaveIdempotencia(clave + "-pago");
        pago.setReferenciaPasarela("seed-" + UUID.randomUUID());
        pago = pagoRepository.save(pago);

        for (int[] item : items) {
            Entrada entrada = new Entrada();
            entrada.setIdEvento(idEvento);
            entrada.setIdCompra(compra.getIdCompra());
            entrada.setIdUsuario(idCliente);
            entrada.setIdSector(idSector);
            entrada.setFila(item[0] == 0 ? null : item[0]);
            entrada.setAsiento(item[1] == 0 ? null : item[1]);
            entrada.setPrecio(precioUnitario);
            entrada.setEstado("VALIDA");
            entrada.setCodigoQr(UUID.randomUUID().toString());
            entrada = entradaRepository.save(entrada);

            RegistroPagoId registroId = new RegistroPagoId();
            registroId.setIdEntrada(entrada.getId());
            registroId.setIdPago(pago.getIdPago());
            RegistroPago registro = new RegistroPago();
            registro.setId(registroId);
            registro.setEntrada(entrada);
            registro.setPago(pago);
            registro.setFechaPago(LocalDateTime.now(ZoneOffset.UTC));
            registro.setDescripcion("Pago de Compra " + compra.getIdCompra() + " - entrada " + entrada.getId());
            registro.setMontoTotal(precioUnitario);
            registroPagoRepository.save(registro);
        }
    }
}
