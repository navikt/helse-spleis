package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning.GraderteAndreYtelserForBeregningPeriode
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.YtelserRiver
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Frilans
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Selvstendig
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

internal class YtelserRiverMappingTest :
    RiverMappingTest<YtelserMessage>(
        hendelse = YtelserMessage::class,
        registrer = { rapid, mediator -> YtelserRiver(rapid, mediator) },
    ) {
    @Test
    fun `Mapping ved løsninger som array`() {
        sendJson(medArray).assertForventetInnhold()
    }

    @Test
    fun `Mapping ved løsninger som object`() {
        sendJson(medObject).assertForventetInnhold()
    }

    @Test
    fun `Mapping ved løsninger uten løsning på forsikring`() {
        val json = fjernLøsninger(medObject, "ForsikringsvurderingResultat")
        sendJson(json).assertForventetInnhold(forsikring = null)
    }

    @Test
    fun `Mapping av graderte andre ytelser med yrkesaktivitet`() =
        Toggle.GraderteAndreYtelser.enable {
            val json =
                medGraderteAndreYtelser(
                    gradertAndreYtelse("FORELDREPENGER", yrkesaktivitet = "987654321"),
                    gradertAndreYtelse("PLEIEPENGER", yrkesaktivitet = "SELVSTENDIG"),
                    gradertAndreYtelse("OMSORGSPENGER", yrkesaktivitet = "FRILANS"),
                )
            val message = checkNotNull(sendMedGraderteAndreYtelserToggle(json)) { "Meldingen ble ikke gjenkjent" }
            assertEquals(
                listOf(
                    GraderteAndreYtelserForBeregning(
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                1.januar,
                                31.januar,
                                50,
                            ),
                        ),
                        GraderteAndreYtelserType.FORELDREPENGER,
                        Arbeidstaker("987654321"),
                    ),
                    GraderteAndreYtelserForBeregning(
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                1.januar,
                                31.januar,
                                50,
                            ),
                        ),
                        GraderteAndreYtelserType.PLEIEPENGER,
                        Selvstendig,
                    ),
                    GraderteAndreYtelserForBeregning(
                        listOf(
                            GraderteAndreYtelserForBeregningPeriode(
                                1.januar,
                                31.januar,
                                50,
                            ),
                        ),
                        GraderteAndreYtelserType.OMSORGSPENGER,
                        Frilans,
                    ),
                ),
                message.graderteAndreYtelser,
            )
        }

    @Test
    fun `Graderte andre ytelser uten yrkesaktivitet avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("FORELDREPENGER", yrkesaktivitet = null))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Graderte andre ytelser med ugyldig yrkesaktivitet avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("FORELDREPENGER", yrkesaktivitet = "a1"))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    private fun gradertAndreYtelse(
        type: String,
        yrkesaktivitet: String?,
    ) = jacksonObjectMapper().createObjectNode().apply {
        put("graderteAndreYtelserType", type)
        if (yrkesaktivitet != null) put("yrkesaktivitet", yrkesaktivitet)
        putArray("graderteAndreYtelserPerioder").addObject().apply {
            put("fom", "2018-01-01")
            put("tom", "2018-01-31")
            put("grad", 50)
        }
    }

    private fun medGraderteAndreYtelser(vararg graderteAndreYtelser: ObjectNode): String {
        val json = jacksonObjectMapper().readTree(medArray)
        val løsninger = json.path("@løsning") as ObjectNode
        løsninger.putArray("GraderteAndreYtelserForBeregning").addAll(graderteAndreYtelser.toList())
        return json.toString()
    }

    // Riveren må registreres mens togglen er på, siden behov og validering settes opp ved registrering
    private fun sendMedGraderteAndreYtelserToggle(json: String): YtelserMessage? {
        var resultat: YtelserMessage? = null
        val mediator =
            object : IMessageMediator {
                override fun onRecognizedMessage(
                    message: HendelseMessage,
                    context: MessageContext,
                ) {
                    resultat = message as YtelserMessage
                }

                override fun onRiverError(
                    riverName: String,
                    problems: MessageProblems,
                    context: MessageContext,
                    metadata: MessageMetadata,
                ) {
                    resultat = null
                }
            }
        TestRapid().apply { YtelserRiver(this, mediator) }.sendTestMessage(json)
        return resultat
    }

    private fun YtelserMessage.assertForventetInnhold(
        forsikring: ForsikringsvurderingResultat? = forventetForsikring,
        forventetOpptjeningsvurderingResultatOk: Boolean? = true,
    ) {
        assertEquals(forventetPleiepenger, this.pleiepenger)
        assertEquals(forventetForeldrepenger, this.foreldrepenger)
        assertEquals(forventetSvangerskapspenger, this.svangerskapspenger)
        assertEquals(forventetOmsorgspenger, this.omsorgspenger)
        assertEquals(forventetOpplæringspenger, this.opplæringspenger)
        assertEquals(forventetInstitusjonsopphold, this.institusjonsopphold)
        assertEquals(forventetInntekterForBeregning, this.inntekterForBeregning)
        assertEquals(forventetArbeidsavklaringspenger, this.arbeidsavklaringspengerV2)
        assertEquals(forventetDagpenger, this.dagpengerV2)
        assertEquals(forsikring, this.forsikringsvurderingResultat)
        assertEquals(forventetOpptjeningsvurderingResultatOk, this.opptjeningsvurderingResultatOk)
    }

    private fun fjernLøsninger(
        json: String,
        vararg fjern: String,
    ): String {
        val json = jacksonObjectMapper().readTree(json)
        val løsninger = json.path("@løsning") as ObjectNode
        løsninger.remove(fjern.toList())
        return json.toString()
    }

    private companion object {
        private val forventetPleiepenger = Pleiepenger(listOf(GradertPeriode(1.januar til 15.januar, 50)))
        private val forventetForeldrepenger = Foreldrepenger(listOf(GradertPeriode(2.januar til 16.januar, 60)))
        private val forventetSvangerskapspenger = Svangerskapspenger(listOf(GradertPeriode(1.juni til 30.juni, 99)))
        private val forventetOmsorgspenger = Omsorgspenger(listOf(GradertPeriode(3.januar til 17.januar, 70)))
        private val forventetOpplæringspenger = Opplæringspenger(listOf(GradertPeriode(4.januar til 18.januar, 80)))
        private val forventetInstitusjonsopphold = Institusjonsopphold(listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.januar, 31.januar)))
        private val forventetInntekterForBeregning = InntekterForBeregning(listOf(InntekterForBeregning.Inntektsperiode("heihei", 1.april til 30.april, 100.daglig)))
        private val forventetArbeidsavklaringspenger = Arbeidsavklaringspenger(listOf(1.mars til 31.mars))
        private val forventetDagpenger = Dagpenger(listOf(1.februar til 28.februar))
        private val forventetForsikring =
            ForsikringsvurderingResultat(
                forsikringsvurderingId = UUID.fromString("abebbf72-2bef-473d-aabb-c5314bcc5ea3"),
                dekning = ForsikringsvurderingResultat.Dekning(grad = 80, iVentetid = true),
                opphørsdato = null,
                harIndividuellForsikring = true,
                villeHattForsikringOmDenVarBetalt = false,
                harForsikringSomIkkePasserMedSøknadstype = false,
            )

        @Language("JSON")
        private val medArray = """
        {
          "@event_name": "behov",
          "@behov": [
            "Foreldrepenger",
            "Pleiepenger",
            "Omsorgspenger",
            "Opplæringspenger",
            "Institusjonsopphold",
            "ArbeidsavklaringspengerV2",
            "InntekterForBeregning",
            "DagpengerV2",
            "ForsikringsvurderingResultat",
            "GraderteAndreYtelserForBeregning",
            "OpptjeningsvurderingResultat"
          ],
          "fødselsnummer": "20014812238",
          "yrkesaktivitetstype": "SELVSTENDIG",
          "organisasjonsnummer": "SELVSTENDIG",
          "@løsning": {
            "Foreldrepenger": {
              "Foreldrepengeytelse": {
                "perioder": [
                  {
                    "fom": "2018-01-02",
                    "tom": "2018-01-16",
                    "grad": 60
                  }
                ]
              },
              "Svangerskapsytelse": {
                "perioder": [
                  {
                    "fom": "2018-06-01",
                    "tom": "2018-06-30",
                    "grad": 99
                  }
                ]
              }
            },
            "Pleiepenger": [
              {
                "fom": "2018-01-01",
                "tom": "2018-01-15",
                "grad": 50
              }
            ],
            "Omsorgspenger": [
              {
                "fom": "2018-01-03",
                "tom": "2018-01-17",
                "grad": 70
              }
            ],
            "Opplæringspenger": [
              {
                "fom": "2018-01-04",
                "tom": "2018-01-18",
                "grad": 80
              }
            ],
            "Institusjonsopphold": [
              {
                "startdato": "2018-01-01",
                "faktiskSluttdato": "2018-01-31",
                "institusjonstype": "fengsel",
                "kategori": "tre"
              }
            ],
            "ArbeidsavklaringspengerV2": {
              "utbetalingsperioder": [
                {
                  "fom": "2018-03-01",
                  "tom": "2018-03-31"
                }
              ]
            },
            "InntekterForBeregning": {
              "inntekter": [
                {
                  "fom": "2018-04-01",
                  "tom": "2018-04-30",
                  "inntektskilde": "heihei",
                  "daglig": 100.0,
                  "måndelig": null,
                  "årlig": null
                }
              ]
            },
            "DagpengerV2": {
              "meldekortperioder": [
                {
                  "fom": "2018-02-01",
                  "tom": "2018-02-28"
                }
              ]
            },
            "ForsikringsvurderingResultat": {
              "forsikringsvurderingId": "abebbf72-2bef-473d-aabb-c5314bcc5ea3",
              "villeHattForsikringOmDenVarBetalt": false,
              "harForsikringSomIkkePasserMedSøknadstype": false,
              "dekning": {
                "grad": 80,
                "iVentetid": true
              },
              "opphørsdato": null,
              "harIndividuellForsikring": true
            },
            "OpptjeningsvurderingResultat": {
              "ok": true
            }
          },
          "@final": true,
          "@besvart": "2026-03-16T10:46:53.61518",
          "vedtaksperiodeId": "0c12ce85-f173-4b1a-b9df-87278315ed93",
          "behandlingId": "73d84eaf-dec6-4a3d-b570-a0598410d2bb",
          "@id": "a1e2253b-f36f-4fba-9515-d553ac2aa07e",
          "@opprettet": "2026-03-16T10:46:53.617425"
        }
        """

        @Language("JSON")
        private val medObject = """
        {
          "@event_name": "behov",
          "@behov": [
            "Foreldrepenger",
            "Pleiepenger",
            "Omsorgspenger",
            "Opplæringspenger",
            "Institusjonsopphold",
            "ArbeidsavklaringspengerV2",
            "InntekterForBeregning",
            "DagpengerV2",
            "ForsikringsvurderingResultat",
            "GraderteAndreYtelserForBeregning",
            "OpptjeningsvurderingResultat"
          ],
          "fødselsnummer": "20014812238",
          "yrkesaktivitetstype": "SELVSTENDIG",
          "organisasjonsnummer": "SELVSTENDIG",
          "@løsning": {
            "Foreldrepenger": {
              "Foreldrepengeytelse": {
                "perioder": [
                  {
                    "fom": "2018-01-02",
                    "tom": "2018-01-16",
                    "grad": 60
                  }
                ]
              },
              "Svangerskapsytelse": {
                "perioder": [
                  {
                    "fom": "2018-06-01",
                    "tom": "2018-06-30",
                    "grad": 99
                  }
                ]
              }
            },
            "Pleiepenger": {
              "perioder": [
                  {
                    "fom": "2018-01-01",
                    "tom": "2018-01-15",
                    "grad": 50
                  }
              ]
            },
            "Omsorgspenger": {
              "perioder": [
                  {
                    "fom": "2018-01-03",
                    "tom": "2018-01-17",
                    "grad": 70
                  }
              ]
            },
            "Opplæringspenger": {
              "perioder": [
                  {
                    "fom": "2018-01-04",
                    "tom": "2018-01-18",
                    "grad": 80
                  }
                ]
            },
            "Institusjonsopphold": {
              "perioder": [
                  {
                    "startdato": "2018-01-01",
                    "faktiskSluttdato": "2018-01-31",
                    "institusjonstype": "fengsel",
                    "kategori": "tre"
                  }
              ]
            },
            "ArbeidsavklaringspengerV2": {
              "utbetalingsperioder": [
                {
                  "fom": "2018-03-01",
                  "tom": "2018-03-31"
                }
              ]
            },
            "InntekterForBeregning": {
              "inntekter": [
                {
                  "fom": "2018-04-01",
                  "tom": "2018-04-30",
                  "inntektskilde": "heihei",
                  "daglig": 100.0,
                  "måndelig": null,
                  "årlig": null
                }
              ]
            },
            "DagpengerV2": {
              "meldekortperioder": [
                {
                  "fom": "2018-02-01",
                  "tom": "2018-02-28"
                }
              ]
            },
            "ForsikringsvurderingResultat": {
              "forsikringsvurderingId": "abebbf72-2bef-473d-aabb-c5314bcc5ea3",
              "villeHattForsikringOmDenVarBetalt": false,
              "harForsikringSomIkkePasserMedSøknadstype": false,
              "dekning": {
                "grad": 80,
                "iVentetid": true
              },
              "opphørsdato": null,
              "harIndividuellForsikring": true
            },
            "OpptjeningsvurderingResultat": {
              "ok": true
            }
          },
          "@final": true,
          "@besvart": "2026-03-16T10:46:53.61518",
          "vedtaksperiodeId": "0c12ce85-f173-4b1a-b9df-87278315ed93",
          "behandlingId": "73d84eaf-dec6-4a3d-b570-a0598410d2bb",
          "@id": "a1e2253b-f36f-4fba-9515-d553ac2aa07e",
          "@opprettet": "2026-03-16T10:46:53.617425"
        }
        """
    }
}
