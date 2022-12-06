package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.TestMessageFactory.Arbeidsgiveropplysning
import no.nav.helse.spleis.TestMessageFactory.Refusjonsopplysning
import no.nav.helse.spleis.TestMessageFactory.Subsumsjon
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT

internal class OverstyrArbeidsgiveropplysningerRiverTest : RiverTest() {

    @Test
    fun `kan mappe melding om overstyring av arbeidsgiveropplysninger for en arbeidsgiver`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = "forklaring",
                    subsumsjon = Subsumsjon("8-15", null, null),
                    refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                ))
            )
        )
    }

    @Test
    fun `kan mappe melding om overstyring av arbeidsgiveropplysninger for flere arbeidsgivere`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(
                    "a1" to Arbeidsgiveropplysning(
                        månedligInntekt = INNTEKT,
                        forklaring = "forklaring",
                        subsumsjon = Subsumsjon("8-15", null, null),
                        refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                    ),
                     "a2" to Arbeidsgiveropplysning(
                         månedligInntekt = INNTEKT,
                         forklaring = "forklaring",
                         subsumsjon = Subsumsjon("8-15", null, null),
                         refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                     )
                )
            )
        )
    }

    @Test
    fun `skal feile om det ikke er noen arbeidsgivere`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                emptyMap()
            )
        )
    }

    @Test
    fun `skal feile om om refusjonsopplysninger er null`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = "forklaring",
                    subsumsjon = Subsumsjon("8-15", null, null),
                    refusjonsopplysninger = null
                ))
            )
        )
    }
    @Test
    fun `refusjonsopplysninger kan være en tom liste`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = "forklaring",
                    subsumsjon = Subsumsjon("8-15", null, null),
                    refusjonsopplysninger = emptyList()
                ))
            )
        )
    }

    @Test
    fun `skal feile hvis vi mangler forklaring fra saksbehandler`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = null,
                    subsumsjon = Subsumsjon("8-15", null, null),
                    refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                ))
            )
        )
    }

    @Test
    fun `skal feile hvis vi mangler opplysninger på èn av fler arbeidsgivere`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(
                    "a1" to Arbeidsgiveropplysning(
                        månedligInntekt = INNTEKT,
                        forklaring = "forklaring",
                        subsumsjon = Subsumsjon("8-15", null, null),
                        refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                    ),
                    "a2" to Arbeidsgiveropplysning(
                        månedligInntekt = INNTEKT,
                        forklaring = " ",
                        subsumsjon = Subsumsjon("8-15", null, null),
                        refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                    )
                )
            )
        )
    }

    @Test
    fun `skal feile hvis vi har en subsumsjon uten paragraf`() {
        assertErrors(
            testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
                1.januar,
                mapOf(ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = "forklaring",
                    subsumsjon = Subsumsjon(null, null, null),
                    refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, 0.0))
                ))
            )
        )
    }

    @Test
    fun `lager riktig format på OverstyrArbeidsgiveropplysninger-hendelsen`() {
        val overstyrArbeidsgiveropplysninger = testMessageFactory.lagOverstyrArbeidsgiveropplysninger(
            1.januar,
            mapOf(
                ORGNUMMER to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT,
                    forklaring = "forklaring",
                    subsumsjon = Subsumsjon("8-15", null, null),
                    refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, 31.januar, INNTEKT/2), Refusjonsopplysning(1.februar, null, 0.0))
                ),
                "987654322" to Arbeidsgiveropplysning(
                    månedligInntekt = INNTEKT/2,
                    forklaring = "forklaring2",
                    subsumsjon = Subsumsjon("8-14", "1", "a"),
                    refusjonsopplysninger = listOf(Refusjonsopplysning(1.januar, null, INNTEKT/3)
                )
            ))
        )

        @Language("json")
        val forventetResultat = """
            {
              "@event_name": "overstyr_arbeidsgiveropplysninger",
              "aktørId": "42",
              "fødselsnummer": "12029240045",
              "skjæringstidspunkt": "2018-01-01",
              "arbeidsgiveropplysninger": {
                "987654321": {
                  "månedligInntekt": 31000.0,
                  "forklaring": "forklaring",
                  "refusjonsopplysninger": [
                    {
                      "fom": "2018-01-01",
                      "tom": "2018-01-31",
                      "beløp": 15500.0
                    },
                    {
                      "fom": "2018-02-01",
                      "beløp": 0.0
                    }
                  ],
                  "subsumsjon": {
                    "paragraf": "8-15"
                  }
                },
                "987654322": {
                  "månedligInntekt": 15500.0,
                  "forklaring": "forklaring2",
                  "refusjonsopplysninger": [
                    {
                      "fom": "2018-01-01",
                      "beløp": 10333.333333333334
                    }
                  ],
                  "subsumsjon": {
                    "paragraf": "8-14",
                    "ledd": "1",
                    "bokstav": "a"
                  }
                }
              }
            }"""

        val faktiskResultat =
            overstyrArbeidsgiveropplysninger.json("@event_name", "aktørId", "fødselsnummer", "skjæringstidspunkt", "arbeidsgiveropplysninger")

        JSONAssert.assertEquals(forventetResultat, faktiskResultat, STRICT)
        assertNoErrors(overstyrArbeidsgiveropplysninger)
    }

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrArbeidsgiveropplysningerRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT,
        LocalDate.of(1992, 2, 12)
    )

    private companion object {
        private val objectMapper = jacksonObjectMapper()
        private fun Pair<String, String>.json(vararg behold: String) = (objectMapper.readTree(second) as ObjectNode).let { json ->
            json.remove(json.fieldNames().asSequence().minus(behold.toSet()).toList())
        }.toString()
    }
}