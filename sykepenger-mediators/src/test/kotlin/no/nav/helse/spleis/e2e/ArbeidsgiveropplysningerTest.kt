package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class ArbeidsgiveropplysningerTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut forventet event TrengerArbeidsgiveropplysninger ved en enkel førstegangsbehandling`() =
        Toggle.Splarbeidsbros.enable {
            sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
            sendSøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
            )
            Assertions.assertEquals(1, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
            val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

            val faktiskResultat = trengerOpplysningerEvent.json(
                "@event_name",
                "organisasjonsnummer",
                "skjæringstidspunkt",
                "sykmeldingsperioder",
                "forespurteOpplysninger",
                "aktørId",
                "fødselsnummer"
            )

            JSONAssert.assertEquals(forventetResultatTrengerInntekt, faktiskResultat, JSONCompareMode.STRICT)
        }

    @Test
    fun `sender ut forventet event TrengerArbeidsgiveropplysninger ved to arbeidsgivere og gap kun hos den ene`() =
        Toggle.Splarbeidsbros.enable {
            val a1 = "ag1"
            val a2 = "ag2"
            nyeVedtakForJanuar(a1, a2)
            forlengMedFebruar(a1)

            sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100), orgnummer = a2)
            sendSøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)),
                orgnummer = a2
            )

            val meldinger = testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver")
            Assertions.assertEquals(3, meldinger.size)
            val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

            val faktiskResultat = trengerOpplysningerEvent.json(
                "@event_name",
                "organisasjonsnummer",
                "skjæringstidspunkt",
                "sykmeldingsperioder",
                "forespurteOpplysninger",
                "aktørId",
                "fødselsnummer"
            )

            JSONAssert.assertEquals(forventetResultatFastsattInntekt, faktiskResultat, JSONCompareMode.STRICT)
        }

    private fun forlengMedFebruar(a1: String) {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)),
            orgnummer = a1
        )
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, orgnummer = a1)
        sendUtbetaling()
    }

    private fun nyeVedtakForJanuar(a1: String, a2: String) {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            orgnummer = a1
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a2)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            orgnummer = a2
        )

        sendInntektsmelding(
            listOf(no.nav.inntektsmeldingkontrakt.Periode(1.januar, 16.januar)),
            1.januar,
            orgnummer = a1
        )
        sendInntektsmelding(
            listOf(no.nav.inntektsmeldingkontrakt.Periode(1.januar, 16.januar)),
            1.januar,
            orgnummer = a2
        )
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = 1.januar,
            orgnummer = a1,
            inntekter = sammenligningsgrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(INNTEKT, a2),
                )
            ),
            arbeidsforhold = listOf(
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                TestMessageFactory.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, a2),
                )
            )
        )
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, orgnummer = a1)
        sendUtbetaling()

        sendYtelser(0, orgnummer = a2)
        sendSimulering(0, orgnummer = a2, status = SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, orgnummer = a2)
        sendUtbetaling()
    }

    @Test
    fun `sender ikke ut event TrengerArbeidsgiveropplysninger med toggle disabled`() = Toggle.Splarbeidsbros.disable {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        )
        assertEquals(0, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
    }

    @Language("json")
    val forventetResultatFastsattInntekt = """
        {
          "@event_name": "trenger_opplysninger_fra_arbeidsgiver",
          "organisasjonsnummer": "ag2",
          "skjæringstidspunkt": "2018-01-01",
          "sykmeldingsperioder": [
            {
              "fom": "2018-03-01",
              "tom": "2018-03-31"
            }
          ],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "FastsattInntekt",
              "fastsattInntekt": 31000.0
            },
            {
              "opplysningstype": "Refusjon",
              "forslag": [
                {
                  "fom": "2018-01-01",
                  "tom": null,
                  "beløp": 31000.0
                }
              ]
            },
            {
              "opplysningstype": "Arbeidsgiverperiode",
              "forslag": [
                {
                  "fom": "2018-03-01",
                  "tom": "2018-03-16"
                }
              ]
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""

    @Language("json")
    val forventetResultatTrengerInntekt = """
        {
          "@event_name": "trenger_opplysninger_fra_arbeidsgiver",
          "organisasjonsnummer": "987654321",
          "skjæringstidspunkt": "2018-01-01",
          "sykmeldingsperioder": [
            {
              "fom": "2018-01-01",
              "tom": "2018-01-31"
            }
          ],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "Inntekt",
              "forslag": {
                "beregningsmåneder": [
                  "2017-10",
                  "2017-11",
                  "2017-12"
                ]
              }
            },
            {
              "opplysningstype": "Refusjon",
              "forslag": []
            },
            {
              "opplysningstype": "Arbeidsgiverperiode",
              "forslag": [
                {
                  "fom": "2018-01-01",
                  "tom": "2018-01-16"
                }
              ]
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""


    private companion object {
        private fun JsonNode.json(vararg behold: String) = (this as ObjectNode).let { json ->
            json.remove(json.fieldNames().asSequence().minus(behold.toSet()).toList())
        }.toString()
    }

}
