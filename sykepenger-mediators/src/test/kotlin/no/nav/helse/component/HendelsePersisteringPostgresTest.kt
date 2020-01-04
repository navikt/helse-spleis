package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.createHikariConfig
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.løsBehov
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.runMigration
import no.nav.helse.spleis.HendelseRecorder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

class HendelsePersisteringPostgresTest {

    companion object {
        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        @BeforeAll
        @JvmStatic
        internal fun `start postgres`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
            runMigration(HikariDataSource(hikariConfig))

        }

        @AfterAll
        @JvmStatic
        internal fun `stop postgres`() {
            postgresConnection.close()
            embeddedPostgres.close()
        }
    }

    @Test
    internal fun `hendelser skal lagres`() {
        val dataSource = HikariDataSource(hikariConfig)
        val dao = HendelseRecorder(dataSource)

        nySøknadHendelse().also {
            dao.onNySøknad(it)
            assertHendelse(dataSource, it)
        }

        sendtSøknadHendelse().also {
            dao.onSendtSøknad(it)
            assertHendelse(dataSource, it)
        }

        inntektsmeldingHendelse().also {
            dao.onInntektsmelding(it)
            assertHendelse(dataSource, it)
        }

        Vilkårsgrunnlag(Vilkårsgrunnlag.lagBehov(UUID.randomUUID(), "aktørid", "fnr", "orgnr").løsBehov(mapOf(
            "EgenAnsatt" to false
        ))).also {
            dao.onVilkårsgrunnlag(it)
            assertHendelse(dataSource, it)
        }
    }

    private fun assertHendelse(dataSource: DataSource, hendelse: ArbeidstakerHendelse) {
        val alleHendelser = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM hendelse WHERE aktor_id = ? AND type = ? ORDER BY id", hendelse.aktørId(), hendelse.hendelsetype().name).map {
                it.string("data")
            }.asList)
        }
        assertEquals(1, alleHendelser.size, "Antall hendelser skal være 1, men var ${alleHendelser.size}")
    }
}
