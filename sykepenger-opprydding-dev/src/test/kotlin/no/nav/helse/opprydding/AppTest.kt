package no.nav.helse.opprydding

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest: DBTest() {
    private lateinit var testRapid: TestRapid

    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        personRepository = PersonRepository(dataSource)
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

    private fun slettemelding(fødselsnummer: String) = JsonMessage.newMessage("slett_person", mapOf("fødselsnummer" to fødselsnummer)).toJson()

    private fun opprettPerson(fødselsnummer: String) {
        opprettDummyPerson(fødselsnummer)
        assertEquals(1, finnPerson(fødselsnummer))
        assertEquals(1, finnMelding(fødselsnummer))
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

    private fun opprettDummyPerson(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                val opprettMelding =
                    "INSERT INTO melding(fnr, melding_id, melding_type, data, behandlet_tidspunkt) VALUES(?, ?, ?, ?::json, ?)"
                it.run(
                    queryOf(opprettMelding, fødselsnummer.toLong(), UUID.randomUUID(), "melding", "{}", LocalDateTime.now()).asUpdate
                )

                val opprettPerson =
                    "INSERT INTO person(skjema_versjon, fnr, aktor_id, data) VALUES(?, ?, ?, ?::json)"
                it.run(
                    queryOf(opprettPerson, 0, fødselsnummer.toLong(), fødselsnummer.reversed().toLong(), "{}").asUpdate
                )
            }
        }
    }


}
