package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.Instant
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import no.nav.helse.spleis.utboks.UtgåendeMeldingTest.Companion.nyUuidv7
import org.junit.jupiter.api.assertThrows

class UtsenderTest {

    @Test
    fun `Meldinger som går bra, og meldinger som går leit`() {
        val okMelding = UtgåendeMelding.nyRapidmelding(
            personidentifikator = Personidentifikator(UNG_PERSON_FNR_2018),
            eventName = "fungerende-event",
            innhold = mapOf("a" to "b")
        )

        val feilMelding = UtgåendeMelding.nyRapidmelding(
            personidentifikator = Personidentifikator(UNG_PERSON_FNR_2018),
            eventName = "feilende-event",
            innhold = mapOf("feil" to "jeg skal feile")
        )

        val okSubumsjon = UtgåendeMelding.nySubsumsjonsmelding(Personidentifikator(UNG_PERSON_FNR_2018)) { _, _ ->
            mapOf("paragraf" to "1")
        }

        val okMeldingUtenKey = UtgåendeMelding(
            key = null,
            json = """{"@id": "${nyUuidv7()}"}""",
            mottaker = UtgåendeMelding.Mottaker.RAPID
        )

        val (sendt, ok, feil) = enTøyseteUtsender.send(listOf(okMelding, feilMelding, okSubumsjon, okMeldingUtenKey))
        assertEquals(3, ok.size)
        assertEquals(1, feil.size)

        assertEquals(okMelding.stappInnSendt(sendt), ok.first())
        assertEquals(okSubumsjon.stappInnSendt(sendt), ok[1])
        assertEquals(okMeldingUtenKey.stappInnSendt(sendt), ok[2])
        assertEquals(feilMelding, feil.single()) // De som feiler har ikke @sendt ettersom de ikke er sendt
    }

        @Test
        fun `Feiler ved duplikate id'er`() {
            val melding1 = UtgåendeMelding.nyRapidmelding(
                personidentifikator = Personidentifikator(UNG_PERSON_FNR_2018),
                eventName = "fungerende-event",
                innhold = mapOf("a" to "b")
            )

            val melding2 = melding1.copy()

            val error = assertThrows<IllegalStateException> {
                enTøyseteUtsender.send(listOf(melding1, melding2))
            }

            assertEquals("Duplikate id'er i utgående meldinger", error.message)
        }

        private companion object {

            private fun UtgåendeMelding.stappInnSendt(sendt: Instant) = copy(json = json.apply {
                put("@sendt", sendt.toString())
            })

            private val enTøyseteUtsender = object : Utsender() {

                override fun utførSending(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): Pair<List<UtgåendeMelding>, List<UtgåendeMelding>> {
                    val (ok, feil) = utgåendeMeldinger.partition { utgåendeMelding ->
                        utgåendeMelding.json.path("feil").isMissingOrNull()
                    }
                    return ok to feil
                }
            }
        }
}
