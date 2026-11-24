package com.ticketya.entrada.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FilaVirtualIndexMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public FilaVirtualIndexMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        // Hibernate completes ddl-auto before ApplicationRunner execution. This
        // transaction-scoped lock serializes DDL when multiple instances start together.
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(1346454346, 0)");
        Integer tablasEsperadas = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM pg_tables
                WHERE schemaname = current_schema()
                  AND tablename IN ('entrada_fila', 'compras', 'pagos')
                """, Integer.class);
        if (tablasEsperadas == null || tablasEsperadas != 3) {
            throw new IllegalStateException("Hibernate todavía no creó las tablas requeridas por la migración");
        }
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_entrada_fila_usuario_vigente
                ON entrada_fila (id_evento, id_usuario)
                WHERE estado IN ('EN_ESPERA', 'HABILITADO')
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_compra_reservada_usuario_evento
                ON compras (id_usuario, id_evento)
                WHERE estado = 'RESERVADA'
                """);
        jdbcTemplate.execute("ALTER TABLE pagos ADD COLUMN IF NOT EXISTS id_compra bigint");
        jdbcTemplate.execute("ALTER TABLE pagos ALTER COLUMN id_entrada DROP NOT NULL");
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_compra_idempotencia
                ON pagos (id_compra, clave_idempotencia)
                WHERE id_compra IS NOT NULL
                """);
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_una_referencia'
                    ) THEN
                        ALTER TABLE pagos ADD CONSTRAINT ck_pago_una_referencia
                        CHECK ((id_entrada IS NOT NULL) <> (id_compra IS NOT NULL));
                    END IF;
                END;
                $$
                """);
    }
}
