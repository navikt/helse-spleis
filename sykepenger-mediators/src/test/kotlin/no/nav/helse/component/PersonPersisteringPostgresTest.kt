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
import no.nav.helse.person.Person
import no.nav.helse.runMigration
import no.nav.helse.spleis.LagrePersonDao
import no.nav.helse.spleis.PersonPostgresRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection

class PersonPersisteringPostgresTest {

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
    internal fun `skal gi null når person ikke finnes`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        assertNull(repo.hentPerson("1"))
    }

    @Test
    internal fun `skal returnere person når person blir lagret etter tilstandsendring`() {
        val dataSource = HikariDataSource(hikariConfig)
        val repo = PersonPostgresRepository(dataSource)

        val person = Person("2", "fnr")
        person.addObserver(LagrePersonDao(dataSource))
        person.håndter(nySøknadHendelse())

        assertNotNull(repo.hentPerson("2"))
    }

    @Test
    internal fun `hver endring av person fører til at ny versjon lagres`() {
        val dataSource = HikariDataSource(hikariConfig)

        val aktørId = "3"
        val person = Person(aktørId, "fnr")
        person.addObserver(LagrePersonDao(dataSource))
        person.håndter(nySøknadHendelse())
        person.håndter(sendtSøknadHendelse())

        val alleVersjoner = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id", aktørId).map {
                Person.restore(Person.Memento.fromString(it.string("data")))
            }.asList)
        }
        assertEquals(2, alleVersjoner.size, "Antall versjoner av personaggregat skal være 2, men var ${alleVersjoner.size}")
    }

}
