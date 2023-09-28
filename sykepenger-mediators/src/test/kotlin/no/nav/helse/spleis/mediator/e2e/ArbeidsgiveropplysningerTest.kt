package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.TestMessageFactory.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class ArbeidsgiveropplysningerTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut forventet event TrengerArbeidsgiveropplysninger ved en enkel førstegangsbehandling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 2.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 2.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            egenmeldingerFraSykmelding = listOf(1.januar)
        )
        Assertions.assertEquals(1, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
        val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

        val faktiskResultat = trengerOpplysningerEvent.json(
            "@event_name",
            "organisasjonsnummer",
            "skjæringstidspunkt",
            "sykmeldingsperioder",
            "egenmeldingsperioder",
            "forespurteOpplysninger",
            "aktørId",
            "fødselsnummer"
        )

        JSONAssert.assertEquals(forventetResultatTrengerInntekt, faktiskResultat, JSONCompareMode.STRICT)
    }

    @Test
    fun `sender ut forventet event TrengerArbeidsgiveropplysninger ved to arbeidsgivere og gap kun hos den ene`() {
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
                "egenmeldingsperioder",
                "forespurteOpplysninger",
                "aktørId",
                "fødselsnummer"
            )

            JSONAssert.assertEquals(forventetResultatFastsattInntekt, faktiskResultat, JSONCompareMode.STRICT)
        }

    @Test
    fun `sender med inntekt fra forrige skjæringstidspunkt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 2.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 2.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            egenmeldingerFraSykmelding = listOf(1.januar)
        )
        sendInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            1.januar
        )
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)),
            egenmeldingerFraSykmelding = emptyList()
        )
        sendInntektsmelding(
            listOf(Periode(1.mars, 16.mars)),
            1.mars
        )
        sendVilkårsgrunnlag(1)
        sendYtelser(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1)
        sendUtbetaling()
        Assertions.assertEquals(2, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
        val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

        val faktiskResultat = trengerOpplysningerEvent.json(
            "@event_name",
            "organisasjonsnummer",
            "skjæringstidspunkt",
            "sykmeldingsperioder",
            "egenmeldingsperioder",
            "forespurteOpplysninger",
            "aktørId",
            "fødselsnummer"
        )

        JSONAssert.assertEquals(forventetResultatMedInntektFraForrigeSkjæringstidpunkt, faktiskResultat, JSONCompareMode.STRICT)
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
            listOf(Periode(1.januar, 16.januar)),
            1.januar,
            orgnummer = a1
        )
        sendInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
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
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                TestMessageFactory.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
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
    fun `sender ut forventet event TrengerArbeidsgiveropplysninger ved førstegangsbehandling med kort gap til forrige`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 10.februar, tom = 10.mars, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 10.februar, tom = 10.mars, sykmeldingsgrad = 100))
        )

        val meldinger = testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver")
        Assertions.assertEquals(2, meldinger.size)
        val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

        val faktiskResultat = trengerOpplysningerEvent.json(
            "@event_name",
            "organisasjonsnummer",
            "skjæringstidspunkt",
            "sykmeldingsperioder",
            "egenmeldingsperioder",
            "forespurteOpplysninger",
            "aktørId",
            "fødselsnummer"
        )

        JSONAssert.assertEquals(forventetResultatKortGap, faktiskResultat, JSONCompareMode.STRICT)
    }

    @Test
    fun `Sender med forrige refusjonsopplysninger i forespørsel`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(
            arbeidsgiverperiode = listOf(Periode(1.januar, 16.januar)),
            1.januar,
            opphørsdatoForRefusjon = 1.april
        )
        sendVilkårsgrunnlag(0)

        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        )

        Assertions.assertEquals(2, testRapid.inspektør.meldinger("trenger_opplysninger_fra_arbeidsgiver").size)
        val trengerOpplysningerEvent = testRapid.inspektør.siste("trenger_opplysninger_fra_arbeidsgiver")

        val faktiskResultat = trengerOpplysningerEvent.json(
            "@event_name",
            "organisasjonsnummer",
            "skjæringstidspunkt",
            "sykmeldingsperioder",
            "egenmeldingsperioder",
            "forespurteOpplysninger",
            "aktørId",
            "fødselsnummer"
        )

        JSONAssert.assertEquals(forventetResultatOpphørAvRefusjon, faktiskResultat, JSONCompareMode.STRICT)
    }

    @Test
    fun `sender ut trenger_ikke_opplysninger_fra_arbeidsgiver ved out-of-order som fører til at vi ikke trenger opplysninger på siste periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))

        Assertions.assertEquals(1, testRapid.inspektør.meldinger("trenger_ikke_opplysninger_fra_arbeidsgiver").size)
        val trengerIkkeOpplysningerEvent = testRapid.inspektør.siste("trenger_ikke_opplysninger_fra_arbeidsgiver")
        assertDoesNotThrow { UUID.fromString(trengerIkkeOpplysningerEvent["vedtaksperiodeId"].asText()) }
        assertTrue(trengerIkkeOpplysningerEvent["organisasjonsnummer"].asText().isNotBlank())
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
          "egenmeldingsperioder": [],
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
              "opplysningstype": "Arbeidsgiverperiode"
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
              "fom": "2018-01-02",
              "tom": "2018-01-31"
            }
          ],
          "egenmeldingsperioder": [
            {
              "fom": "2018-01-01",
              "tom": "2018-01-01"
            }
          ],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "Inntekt",
              "forslag": {
                "forrigeInntekt": null
              }
            },
            {
              "opplysningstype": "Refusjon",
              "forslag": []
            },
            {
              "opplysningstype": "Arbeidsgiverperiode"
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""

    @Language("json")
    val forventetResultatOpphørAvRefusjon = """
        {
          "@event_name": "trenger_opplysninger_fra_arbeidsgiver",
          "organisasjonsnummer": "987654321",
          "skjæringstidspunkt": "2018-03-01",
          "sykmeldingsperioder": [
            {
              "fom": "2018-03-01",
              "tom": "2018-03-31"
            }
          ],
          "egenmeldingsperioder": [],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "Inntekt",
              "forslag": {
                "forrigeInntekt": {
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INNTEKTSMELDING",
                  "beløp": 31000.0
                }
              }
            },
            {
              "opplysningstype": "Refusjon",
              "forslag": [
                {
                  "fom": "2018-01-01",
                  "tom": "2018-04-01", 
                  "beløp": 31000.0
                },
                {
                  "fom": "2018-04-02",
                  "tom": null, 
                  "beløp": 0.0
                }
              ]
            },
            {
              "opplysningstype": "Arbeidsgiverperiode"
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""

    @Language("json")
    val forventetResultatMedInntektFraForrigeSkjæringstidpunkt = """
        {
          "@event_name": "trenger_opplysninger_fra_arbeidsgiver",
          "organisasjonsnummer": "987654321",
          "skjæringstidspunkt": "2018-03-01",
          "sykmeldingsperioder": [
            {
              "fom": "2018-03-01",
              "tom": "2018-03-31"
            }
          ],
          "egenmeldingsperioder": [],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "Inntekt",
              "forslag": {
                "forrigeInntekt": {
                  "skjæringstidspunkt": "2018-01-01", 
                  "kilde": "INNTEKTSMELDING", 
                  "beløp": 31000.0
                }
              }
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
              "opplysningstype": "Arbeidsgiverperiode"
            }
          ],
          "aktørId": "42",
          "fødselsnummer": "12029240045"
        }"""

    @Language("json")
    val forventetResultatKortGap = """
        {
          "@event_name": "trenger_opplysninger_fra_arbeidsgiver",
          "organisasjonsnummer": "987654321",
          "skjæringstidspunkt": "2018-02-10",
          "sykmeldingsperioder": [
            {
              "fom": "2018-01-01",
              "tom": "2018-01-31"
            }, 
            {
              "fom": "2018-02-10",
              "tom": "2018-03-10"
            }

          ],
          "egenmeldingsperioder": [],
          "forespurteOpplysninger": [
            {
              "opplysningstype": "Inntekt",
              "forslag": {
                "forrigeInntekt": {
                  "skjæringstidspunkt": "2018-01-01",
                  "kilde": "INNTEKTSMELDING",
                  "beløp": 31000.0
                }
              }
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
