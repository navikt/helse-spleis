package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.int
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.util.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest : DBTest() {

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
        personRepository.slett("123")
        assertEquals(0, finnPerson("123"))
        assertEquals(0, finnMelding("123"))
    }

    private fun finnPerson(fødselsnummer: String): Int {
        return dataSource.connection {
            prepareStatementWithNamedParameters("SELECT COUNT(1) FROM person WHERE fnr = :fnr") {
                withParameter("fnr", fødselsnummer.toLong())
            }.use {
                it.executeQuery().use { rs ->
                    rs.single { it.int(1) }
                }
            }
        }
    }

    private fun finnMelding(fødselsnummer: String): Int {
        return dataSource.connection {
            prepareStatementWithNamedParameters("SELECT COUNT(1) FROM melding WHERE fnr = :fnr") {
                withParameter("fnr", fødselsnummer.toLong())
            }.use {
                it.executeQuery().use { rs ->
                    rs.single { it.int(1) }
                }
            }
        }
    }

    private fun opprettDummyPerson(fødselsnummer: String) {
        dataSource.connection {
            transaction {
                @Language("PostgreSQL")
                val opprettMelding = "INSERT INTO melding(fnr, melding_id, melding_type, data, behandlet_tidspunkt) VALUES(:fnr, :meldingId, :meldingType, cast(:data as json), now())"
                prepareStatementWithNamedParameters(opprettMelding) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("meldingId", UUID.randomUUID())
                    withParameter("meldingType", "melding")
                    withParameter("data", "{}")
                }.use { it.execute() }
                @Language("PostgreSQL")
                val opprettPerson = "INSERT INTO person(skjema_versjon, fnr, data) VALUES(0, :fnr, '{}')"
                prepareStatementWithNamedParameters(opprettPerson) {
                    withParameter("fnr", fødselsnummer.toLong())
                }.use {
                    it.execute()
                }
            }

        }
    }
}
