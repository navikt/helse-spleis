package no.nav.helse.opprydding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest: DBTest() {

    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun `start postgres`() {
        personRepository = PersonRepository(dataSource)
    }

    @Test
    fun `Kan slette person`() {
        opprettDummyPerson("123")
        assertEquals(1, finnPerson("123"))
        assertEquals(1, finnMelding("123"))
        assertEquals(1, finnUnikePerson("123"))
        personRepository.slett("123")
        assertEquals(0, finnPerson("123"))
        assertEquals(0, finnMelding("123"))
        assertEquals(0, finnUnikePerson("123"))
    }

    private fun runMigration(psql: PostgreSQLContainer<Nothing>): DataSource {
        val dataSource = HikariDataSource(createHikariConfig(psql))
        Flyway.configure()
            .dataSource(dataSource)
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load()
            .also { it.clean() }
            .migrate()
        return dataSource
    }

    private fun createHikariConfig(psql: PostgreSQLContainer<Nothing>) =
        HikariConfig().apply {
            this.jdbcUrl = psql.jdbcUrl
            this.username = psql.username
            this.password = psql.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            initializationFailTimeout = 5000
            maxLifetime = 30001
        }

    private fun finnPerson(fødselsnummer: String): Int {
        return sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT COUNT(1) FROM person WHERE fnr = ?", fødselsnummer.toLong()).map { it.int(1) }.asSingle)
        } ?: 0
    }

    private fun finnMelding(fødselsnummer: String): Int {
        return sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT COUNT(1) FROM melding WHERE fnr = ?", fødselsnummer.toLong()).map { it.int(1) }.asSingle)
        } ?: 0
    }

    private fun finnUnikePerson(fødselsnummer: String): Int {
        return sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT COUNT(1) FROM unike_person WHERE fnr = ?", fødselsnummer.toLong()).map { it.int(1) }.asSingle)
        } ?: 0
    }

    private fun opprettDummyPerson(fødselsnummer: String) {
        sessionOf(dataSource).transaction {
            val opprettMelding =
                "INSERT INTO melding(fnr, melding_id, melding_type, data, behandlet_tidspunkt) VALUES(?, ?, ?, ?::json, ?)"
            it.run(
                queryOf(
                    opprettMelding,
                    fødselsnummer.toLong(),
                    UUID.randomUUID(),
                    "melding",
                    "{}",
                    LocalDateTime.now()
                ).asExecute
            )

            val opprettUnikePerson = "INSERT INTO unike_person(fnr, aktor_id, sist_avstemt) VALUES(?, ?, ?)"
            it.run(
                queryOf(
                    opprettUnikePerson,
                    fødselsnummer.toLong(),
                    fødselsnummer.reversed().toLong(),
                    LocalDateTime.now(),
                ).asExecute
            )

            val opprettPerson =
                "INSERT INTO person(skjema_versjon, fnr, data) VALUES(?, ?, ?::json)"
            it.run(
                queryOf(
                    opprettPerson,
                    0,
                    fødselsnummer.toLong(),
                    "{}"
                ).asExecute
            )
        }
    }
}
