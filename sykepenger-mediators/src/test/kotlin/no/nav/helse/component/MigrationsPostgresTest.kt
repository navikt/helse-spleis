package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.createHikariConfig
import no.nav.helse.runMigration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection

// denne testen kan feile pga tilgangskontroll på mapper under /var/folders/ly/5p_0fdf10sv6y_vl408hlhvr0000gp/T/embedded-pg eller tilsvarende
// prøv først å slette denne mappen før du sletter hele testen.
class MigrationsPostgresTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    private lateinit var hikariConfig: HikariConfig

    @BeforeEach
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder()
                .start()

        postgresConnection = embeddedPostgres.postgresDatabase.connection

        hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
    }

    @AfterEach
    fun `stop postgres`() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun `migreringer skal kjøre på en tom database`() {
        val migrations = runMigration(HikariDataSource(hikariConfig))
        assertTrue(migrations > 0, "Ingen migreringer ble kjørt")
    }

    @Test
    fun `migreringer skal ikke kjøres flere ganger`() {
        runMigration(HikariDataSource(hikariConfig))

        val migrations = runMigration(HikariDataSource(hikariConfig))
        assertEquals(0, migrations)
    }
}
