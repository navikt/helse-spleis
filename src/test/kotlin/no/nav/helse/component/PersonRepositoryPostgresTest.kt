package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.TestConstants
import no.nav.helse.createHikariConfig
import no.nav.helse.person.PersonPostgresRepository
import no.nav.helse.person.domain.Person
import no.nav.helse.sakskompleks.db.runMigration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection

class PersonRepositoryPostgresTest {

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
    internal fun `skal returnere person når person finnes`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        val person = Person("2")
        repo.lagrePerson(person)
        assertNotNull(repo.hentPerson("2"))
    }

    @Test
    internal fun `det lagres to ulike versjoner av samme personaggregat`() {
        val dataSource = HikariDataSource(hikariConfig)
        val repo = PersonPostgresRepository(dataSource)

        val aktørId = "3"
        val personAggregat = Person(aktørId)
        repo.lagrePerson(personAggregat)
        repo.lagrePerson(personAggregat)

        val alleVersjoner = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id", aktørId).map {
                Person.fromJson(it.string("data"))
            }.asList)
        }
        assertEquals(2, alleVersjoner.size, "Antall versjoner av personaggregat skal være 2, men var ${alleVersjoner.size}")
    }

    @Test
    internal fun `siste versjon av personaggregat hentes`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        val personAggregat = Person("4")
        repo.lagrePerson(personAggregat)
        personAggregat.håndterNySøknad(TestConstants.nySøknad(id="UnikID"))
        repo.lagrePerson(personAggregat)

        assertTrue(repo.hentPerson("4")!!.toJson().contains("UnikID"))
    }




}
