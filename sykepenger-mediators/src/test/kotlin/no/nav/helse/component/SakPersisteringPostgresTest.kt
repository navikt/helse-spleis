package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.createHikariConfig
import no.nav.helse.runMigration
import no.nav.helse.sak.Sak
import no.nav.helse.spleis.LagreSakDao
import no.nav.helse.spleis.SakPostgresRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection

class SakPersisteringPostgresTest {

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
    internal fun `skal gi null når sak ikke finnes`() {
        val repo = SakPostgresRepository(HikariDataSource(hikariConfig))

        assertNull(repo.hentSak("1", ""))
    }

    @Test
    internal fun `skal returnere sak når sak blir lagret etter statechange`() {
        val dataSource = HikariDataSource(hikariConfig)
        val repo = SakPostgresRepository(dataSource)

        val sak = Sak("2", "fnr")
        sak.addObserver(LagreSakDao(dataSource))
        sak.håndter(nySøknadHendelse())

        assertNotNull(repo.hentSak("2", ""))
    }

    @Test
    internal fun `hver endring av sak fører til at ny versjon lagres`() {
        val dataSource = HikariDataSource(hikariConfig)

        val aktørId = "3"
        val sak = Sak(aktørId, "fnr")
        sak.addObserver(LagreSakDao(dataSource))
        sak.håndter(nySøknadHendelse())
        sak.håndter(sendtSøknadHendelse())

        val alleVersjoner = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id", aktørId).map {
                Sak.restore(Sak.Memento.fromString(it.string("data"), ""))
            }.asList)
        }
        assertEquals(2, alleVersjoner.size, "Antall versjoner av sakaggregat skal være 2, men var ${alleVersjoner.size}")
    }

}
