package no.nav.helse.spleis.mediator.meldinger

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.spleis.meldinger.VilkårsgrunnlagRiver
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkårsgrunlagRiverMappingTest: RiverMappingTest<VilkårsgrunnlagMessage>(
    hendelse = VilkårsgrunnlagMessage::class,
    registrer = { rapid, mediator -> VilkårsgrunnlagRiver(rapid, mediator) }
) {

    @Test
    fun `Mapping ved løsninger som array`() {
        sendJson(medArray).assertForventetInnhold()
    }

    @Test
    fun `Mapping ved løsninger som object`() {
        sendJson(medObject).assertForventetInnhold()
    }

    private fun VilkårsgrunnlagMessage.assertForventetInnhold() {
        assertEquals(forventetInntekterForOpptjening, this.inntekterForOpptjeningsvurdering)
        assertEquals(forventetInntekterForSykepengegrunnlag, this.inntekterForSykepengegrunnlag)
        assertEquals(forventetArbeidsforhold, this.arbeidsforhold)
        assertEquals(forventetMedlemskap, this.medlemskapstatus)
    }

    private companion object {

        private val forventetInntekterForOpptjening = listOf(ArbeidsgiverInntekt(
            arbeidsgiver = "987654321",
            inntekter = listOf(ArbeidsgiverInntekt.MånedligInntekt(
                yearMonth = YearMonth.parse("2017-12"),
                inntekt = 384_000.årlig,
                fordel = "kontantytelse",
                beskrivelse = "fastloenn",
                type = LØNNSINNTEKT
            ))
        ))
        private val forventetInntekterForSykepengegrunnlag = listOf(ArbeidsgiverInntekt(
            arbeidsgiver = "987654322",
            inntekter = listOf(ArbeidsgiverInntekt.MånedligInntekt(
                yearMonth = YearMonth.parse("2017-12"),
                inntekt = 384_000.årlig,
                fordel = "kontantytelse",
                beskrivelse = "fastloenn",
                type = LØNNSINNTEKT
            ))
        ))
        private val forventetArbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(
            orgnummer = "987654321",
            ansattFom = LocalDate.EPOCH,
            ansattTom = null,
            type = ORDINÆRT
        ))
        private val forventetMedlemskap = Medlemskapsvurdering.Medlemskapstatus.Ja

        @Language("JSON")
        private val medArray = """
        {
          "@event_name": "behov",
          "InntekterForSykepengegrunnlag": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "InntekterForOpptjeningsvurdering": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "ArbeidsforholdV2": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "Medlemskap": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "@behov": [
            "Medlemskap",
            "InntekterForSykepengegrunnlag",
            "InntekterForOpptjeningsvurdering",
            "ArbeidsforholdV2"
          ],
          "fødselsnummer": "12029240045",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "organisasjonsnummer": "987654321",
          "@løsning": {
            "Medlemskap": {
              "resultat": {
                "svar": "JA"
              }
            },
            "InntekterForSykepengegrunnlag": [
              {
                "årMåned": "2017-12",
                "inntektsliste": [
                  {
                    "beløp": 32000.0,
                    "inntektstype": "LOENNSINNTEKT",
                    "orgnummer": "987654322",
                    "fordel": "kontantytelse",
                    "beskrivelse": "fastloenn"
                  }
                ]
              }
            ],
            "InntekterForOpptjeningsvurdering": [
              {
                "årMåned": "2017-12",
                "inntektsliste": [
                  {
                    "beløp": 32000.0,
                    "inntektstype": "LOENNSINNTEKT",
                    "orgnummer": "987654321",
                    "fordel": "kontantytelse",
                    "beskrivelse": "fastloenn"
                  }
                ]
              }
            ],
            "ArbeidsforholdV2": [
              {
                "orgnummer": "987654321",
                "ansattSiden": "1970-01-01",
                "ansattTil": null,
                "type": "ORDINÆRT"
              }
            ]
          },
          "@final": true,
          "@besvart": "2026-03-16T09:02:59.875046",
          "vedtaksperiodeId": "002d14c7-ceaf-4aae-972a-fa345f40525f",
          "behandlingId": "2cd328f5-0c07-44ee-8b2a-dc76c907a3fc",
          "@id": "a4663d31-7603-422b-8554-da19498184ca",
          "@opprettet": "2026-03-16T09:03:00.359354"
        }
        """

        @Language("JSON")
        private val medObject = """
        {
          "@event_name": "behov",
          "InntekterForSykepengegrunnlag": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "InntekterForOpptjeningsvurdering": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "ArbeidsforholdV2": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "Medlemskap": {
            "skjæringstidspunkt": "2018-01-01"
          },
          "@behov": [
            "Medlemskap",
            "InntekterForSykepengegrunnlag",
            "InntekterForOpptjeningsvurdering",
            "ArbeidsforholdV2"
          ],
          "fødselsnummer": "12029240045",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "organisasjonsnummer": "987654321",
          "@løsning": {
            "Medlemskap": {
              "resultat": {
                "svar": "JA"
              }
            },
            "InntekterForSykepengegrunnlag": {
                "inntekter": [
                  {
                    "årMåned": "2017-12",
                    "inntektsliste": [
                      {
                        "beløp": 32000.0,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "987654322",
                        "fordel": "kontantytelse",
                        "beskrivelse": "fastloenn"
                      }
                    ]
                  }
              ]
            },
            "InntekterForOpptjeningsvurdering": {
                "inntekter": [
                  {
                    "årMåned": "2017-12",
                    "inntektsliste": [
                      {
                        "beløp": 32000.0,
                        "inntektstype": "LOENNSINNTEKT",
                        "orgnummer": "987654321",
                        "fordel": "kontantytelse",
                        "beskrivelse": "fastloenn"
                      }
                    ]
                  }
              ]
            },
            "ArbeidsforholdV2": {
                "arbeidsforhold": [
                  {
                    "orgnummer": "987654321",
                    "ansattSiden": "1970-01-01",
                    "ansattTil": null,
                    "type": "ORDINÆRT"
                  }
                ]
            }
          },
          "@final": true,
          "@besvart": "2026-03-16T09:02:59.875046",
          "vedtaksperiodeId": "002d14c7-ceaf-4aae-972a-fa345f40525f",
          "behandlingId": "2cd328f5-0c07-44ee-8b2a-dc76c907a3fc",
          "@id": "a4663d31-7603-422b-8554-da19498184ca",
          "@opprettet": "2026-03-16T09:03:00.359354"
        }
        """
    }
}
