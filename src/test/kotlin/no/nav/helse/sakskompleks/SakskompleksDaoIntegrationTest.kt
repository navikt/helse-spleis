package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.createHikariConfig
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.db.runMigration
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.*

class SakskompleksDaoIntegrationTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    private lateinit var hikariConfig: HikariConfig

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSykmelding = SykmeldingMessage(objectMapper.readTree("/sykmelding.json".readResource()))
    }

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
    fun `skal finne sak for bruker`() {
        val sakForBruker = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "123456789"
        )
        val sakForAnnenBruker = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "987654321"
        )

        val dataSource = HikariDataSource(hikariConfig)

        runMigration(dataSource)

        val dao = SakskompleksDao(dataSource)

        dao.opprettSak(sakForBruker)
        dao.opprettSak(sakForAnnenBruker)

        val sakerForBruker = dao.finnSaker("123456789")

        assertEquals(1, sakerForBruker.size)
        assertEquals(sakForBruker, sakerForBruker[0])
    }

    @Test
    fun `skal oppdatere sak`() {
        val aktørId = "987654321"

        val sak = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = aktørId
        )

        val dataSource = HikariDataSource(hikariConfig)

        runMigration(dataSource)

        val dao = SakskompleksDao(dataSource)

        dao.opprettSak(sak)

        var saker = dao.finnSaker(aktørId)
        assertEquals(1, saker.size)
        assertEquals(sak, saker[0])

        sak.leggTil(testSykmelding.sykmelding)

        assertEquals(1, dao.oppdaterSak(sak))

        saker = dao.finnSaker(aktørId)
        assertEquals(1, saker.size)
        assertEquals(sak, saker[0])
    }
}
