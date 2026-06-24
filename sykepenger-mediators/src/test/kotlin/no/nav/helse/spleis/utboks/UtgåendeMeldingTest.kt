package no.nav.helse.spleis.utboks

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.spleis.Behov
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UtgåendeMeldingTest {

    @Test
    fun `Må være en uuidv7 id`() {
        val error = assertThrows<IllegalStateException> {
            UtgåendeMelding(
                key = "11234",
                json = """{"@id": "${UUID.randomUUID()}"}""",
                mottaker = UtgåendeMelding.Mottaker.RAPID
            )
        }
        assertEquals("id må være en UUIDv7", error.message)
    }

    @Test
    fun `Om fødselsnummer er satt i meldingen må key'en være samme`() {
        val errorKeyIkkeSatt = assertThrows<IllegalStateException> {
            UtgåendeMelding(
                key = null,
                json = """{"@id": "${nyUuidv7()}", "fødselsnummer": "11234"}""",
                mottaker = UtgåendeMelding.Mottaker.RAPID
            )
        }
        assertEquals("Når det er satt fødselsnummer i meldingen må den keyes på fødselsnummeret!", errorKeyIkkeSatt.message)

        val errorKeySattFeil = assertThrows<IllegalStateException> {
            UtgåendeMelding(
                key = "11235",
                json = """{"@id": "${nyUuidv7()}", "fødselsnummer": "11234"}""",
                mottaker = UtgåendeMelding.Mottaker.RAPID
            )
        }
        assertEquals("Når det er satt fødselsnummer i meldingen må den keyes på fødselsnummeret!", errorKeySattFeil.message)
    }

    @Test
    fun `Mapper rapidmelding`() {
        val melding = UtgåendeMelding.nyRapidmelding(
            personidentifikator = Personidentifikator(UNG_PERSON_FNR_2018),
            eventName = "fungerende-event",
            innhold = mapOf("a" to "b")
        )
        melding.assertStandardfelter("fungerende-event")
        assertEquals("b", melding.json.path("a").asText())
        assertEquals(UtgåendeMelding.Mottaker.RAPID, melding.mottaker)
    }

    @Test
    fun `Mapper rapidmelding uten fødselsnummer`() {
        val melding = UtgåendeMelding.nyRapidmelding(
            eventName = "fungerende-event-uten-fnr",
            innhold = mapOf("a" to "b")
        )
        melding.assertStandardfelter("fungerende-event-uten-fnr", medFødselsnummer = false)
        assertEquals("b", melding.json.path("a").asText())
        assertEquals(UtgåendeMelding.Mottaker.RAPID, melding.mottaker)
    }

    @Test
    fun `Mapper subsumsjonmelding`() {
        val melding = UtgåendeMelding.nySubsumsjonsmelding(Personidentifikator(UNG_PERSON_FNR_2018)) { id, tidsstempel ->
            mapOf(
                "paragraf" to "1",
                "subsumsjon" to mapOf(
                    "fodselsnummer" to UNG_PERSON_FNR_2018,
                    "id" to id,
                    "tidsstempel" to tidsstempel
                )
            )
        }
        melding.assertStandardfelter("subsumsjon")
        assertEquals("1", melding.json.path("paragraf").asText())
        assertEquals(UNG_PERSON_FNR_2018, melding.json.path("subsumsjon").path("fodselsnummer").asText())
        assertEquals(UUID.fromString(melding.json.path("@id").asText()), UUID.fromString(melding.json.path("subsumsjon").path("id").asText()))
        assertEquals(UtgåendeMelding.Mottaker.SUBSUMSJON, melding.mottaker)

        val tidsstempelZoned = ZonedDateTime.parse(melding.json.path("subsumsjon").path("tidsstempel").asText())
        val tidsstempelUtc = Instant.parse(melding.json.path("@opprettetUTC").asText()).atZone(tidsstempelZoned.zone)
        assertEquals(tidsstempelZoned, tidsstempelUtc)
    }

    @Test
    fun `Mapper behov`() {
        val meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())

        val behov = UtgåendeMelding.nyttBehov(
            personidentifikator = Personidentifikator(UNG_PERSON_FNR_2018),
            meldingsreferanseId = meldingsreferanseId,
            behov = listOf(
                Behov(Behov.Behovstype.Medlemskap, mapOf("a" to "b")),
                Behov(Behov.Behovstype.Simulering, mapOf("c" to "d")),
            ),
            extra = mapOf("e" to "f")
        )
        behov.assertStandardfelter("behov")
        assertDoesNotThrow { UUID.fromString(behov.json.path("@behovId").asText()) }
        assertEquals("behov", behov.json.path("@event_name").asText())
        assertEquals(listOf("Medlemskap", "Simulering"), behov.json.path("@behov").map { it.asText() })
        assertEquals("""{"a":"b"}""", behov.json.path("Medlemskap").toString())
        assertEquals("""{"c":"d"}""", behov.json.path("Simulering").toString())
        assertEquals("f", behov.json.path("e").asText())
        assertEquals(meldingsreferanseId.id, UUID.fromString(behov.json.path("meldingsreferanseId").asText()))
        assertEquals(UtgåendeMelding.Mottaker.RAPID, behov.mottaker)
    }

    internal companion object {
        @OptIn(ExperimentalUuidApi::class)
        internal fun nyUuidv7() = Uuid.generateV7().toString()

        private fun UtgåendeMelding.assertStandardfelter(eventName: String, medFødselsnummer: Boolean = true) {
            assertDoesNotThrow { UUID.fromString(json.path("@id").asText()) }
            assertDoesNotThrow {
                val opprettet = LocalDateTime.parse(json.path("@opprettet").asText())
                val opprettetUTC = Instant.parse(json.path("@opprettetUTC").asText())
                assertEquals(opprettetUTC, opprettet.atZone(ZoneId.systemDefault()).toInstant())
            }
            assertEquals(eventName, json.path("@event_name").asText())
            if (medFødselsnummer) assertEquals(UNG_PERSON_FNR_2018, json.path("fødselsnummer").asText())
        }
    }
}
