package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest {
    private lateinit var testRapid: TestRapid
    private lateinit var psqlContainer: PostgreSQLContainer<Nothing>
    private lateinit var dataSource: DataSource
    private lateinit var personRepository: PersonRepository

    @BeforeAll
    fun beforeAll() {
        psqlContainer = PostgreSQLContainer<Nothing>("postgres:12")
        psqlContainer.start()
    }

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        dataSource = runMigration(psqlContainer)
        personRepository = PersonRepository(dataSource)
        SlettPersonRiver(testRapid, personRepository)
    }

    @AfterAll
    fun afterAll() {
        psqlContainer.close()
    }

    @Test
    fun `slettemelding medfører at person slettes fra databasen`() {
        opprettPerson("123")
        testRapid.sendTestMessage(slettemelding("123"))
        assertEquals(0, finnPerson("123"))
        assertEquals(0, finnMelding("123"))
        assertEquals(0, finnUnikePerson("123"))
    }

    @Language("JSON")
    private fun slettemelding(fødselsnummer: String) = """
        {
          "@event_name": "slett_person",
          "@id": "${UUID.randomUUID()}",
          "opprettet": "${LocalDateTime.now()}",
          "fødselsnummer": "$fødselsnummer"
        }
    """.trimIndent()

    private fun opprettPerson(fødselsnummer: String) {
        opprettDummyPerson(fødselsnummer)
        assertEquals(1, finnPerson(fødselsnummer))
        assertEquals(1, finnMelding(fødselsnummer))
        assertEquals(1, finnUnikePerson(fødselsnummer))
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
                "INSERT INTO person(skjema_versjon, fnr, aktor_id, data, melding_id, rullet_tilbake) VALUES(?, ?, ?, ?::json, ?, ?)"
            it.run(
                queryOf(
                    opprettPerson,
                    0,
                    fødselsnummer.toLong(),
                    fødselsnummer.reversed().toLong(),
                    "{}",
                    UUID.randomUUID(),
                    LocalDateTime.now()
                ).asExecute
            )
        }
    }

    private fun runMigration(psql: PostgreSQLContainer<Nothing>): DataSource {
        val dataSource = HikariDataSource(createHikariConfig(psql))
        Flyway.configure()
            .initSql(dropTables)
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
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
            maxLifetime = 30001
        }

    @Language("SQL")
    private val dropTables = """
    DROP SCHEMA public CASCADE;
    CREATE SCHEMA public;
""".trimIndent()
}