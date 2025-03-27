package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.int
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.util.UUID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AppTest : DBTest() {
    private lateinit var testRapid: TestRapid

    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        personRepository = PersonRepository(dataSource.ds)
        SlettPersonRiver(testRapid, personRepository)
    }

    @Test
    fun `slettemelding medfører at person slettes fra databasen`() {
        opprettPerson("123")
        testRapid.sendTestMessage(slettemelding("123"))
        assertEquals(0, finnPerson("123"))
        assertEquals(0, finnMelding("123"))
    }

    @Test
    fun `sletter kun aktuelt fnr`() {
        opprettPerson("123")
        opprettPerson("1234")
        testRapid.sendTestMessage(slettemelding("123"))
        assertEquals(0, finnPerson("123"))
        assertEquals(1, finnPerson("1234"))
        assertEquals(0, finnMelding("123"))
        assertEquals(1, finnMelding("1234"))
    }

    @Test
    fun `sender kvittering etter at slettingen er gjort`() {
        opprettPerson("123")
        testRapid.sendTestMessage(slettemelding("123"))
        assertEquals(0, finnPerson("123"))
        val kvittering = testRapid.inspektør.message(0)
        assertEquals("person_slettet", kvittering["@event_name"].asText())
        assertEquals("123", kvittering["fødselsnummer"].asText())
    }

    private fun slettemelding(fødselsnummer: String) = JsonMessage.newMessage("slett_person", mapOf("fødselsnummer" to fødselsnummer)).toJson()

    private fun opprettPerson(fødselsnummer: String) {
        opprettDummyPerson(fødselsnummer)
        assertEquals(1, finnPerson(fødselsnummer))
        assertEquals(1, finnMelding(fødselsnummer))
    }

    private fun finnPerson(fødselsnummer: String): Int {
        return dataSource.ds.connection {
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
        return dataSource.ds.connection {
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
        dataSource.ds.connection {
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
