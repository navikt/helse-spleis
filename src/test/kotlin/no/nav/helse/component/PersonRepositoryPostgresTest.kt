package no.nav.helse.component

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.createHikariConfig
import no.nav.helse.person.PersonPostgresRepository
import no.nav.helse.person.domain.Person
import no.nav.helse.sakskompleks.db.runMigration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
}
