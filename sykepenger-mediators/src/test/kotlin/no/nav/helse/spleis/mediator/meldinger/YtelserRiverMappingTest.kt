package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.*
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.serde.spleisJsonMapperBuilder
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.YtelserRiver
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidsledig
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Frilans
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Selvstendig
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.ObjectNode
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
    fun `Mapping uten løsning på medlemskapsvurderingresultat`() {
        assertNull(sendJson(medObject).medlemskapsvurderingResultat)
    }

    @Test
    fun `Mapping av løsning på medlemskapsvurderingresultat`() {
        mapOf(
            "JA" to Medlemskapsvurdering.Medlemskapstatus.Ja,
            "NEI" to Medlemskapsvurdering.Medlemskapstatus.Nei,
            "UAVKLART" to Medlemskapsvurdering.Medlemskapstatus.VetIkke,
            "UAVKLART_MED_BRUKERSPORSMAAL" to Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål,
        ).forEach { (svar, forventet) ->
            val json = medMedlemskapsvurderingResultat(medObject, svar)
            sendJson(json).apply {
                assertForventetInnhold()
                assertEquals(forventet, medlemskapsvurderingResultat)
            }
        }
    }

    @Test
    fun `Mapping uten løsning på rettighetskategoriresultat`() {
        assertNull(sendJson(medObject).rettighetskategoriResultat)
    }

    @Test
    fun `Mapping av løsning på rettighetskategoriresultat`() {
        Rettighetskategori.Hovedkategori.entries.forEach { hovedkategori ->
            val json = medRettighetskategoriResultat(medObject, hovedkategori.name)
            sendJson(json).apply {
                assertForventetInnhold()
                assertEquals(hovedkategori, rettighetskategoriResultat)
            }
        }
    }

    @Test
    fun `Mapping av graderte andre ytelser for alle yrkesaktivitetstyper`() =
        Toggle.GraderteAndreYtelser.enable {
            val json =
                medGraderteAndreYtelser(
                    gradertAndreYtelse("ARBEIDSTAKER", organisasjonsnummer = "987654321", versjon = versjon1),
                    gradertAndreYtelse("FRILANS", organisasjonsnummer = null, versjon = versjon2),
                    gradertAndreYtelse("SELVSTENDIG", organisasjonsnummer = null, versjon = versjon3),
                    gradertAndreYtelse("ARBEIDSLEDIG", organisasjonsnummer = null, versjon = versjon4),
                )
            val message = checkNotNull(sendMedGraderteAndreYtelserToggle(json)) { "Meldingen ble ikke gjenkjent" }
            assertEquals(
                listOf(
                    forventetGradertAndreYtelse(Arbeidstaker("987654321"), versjon1),
                    forventetGradertAndreYtelse(Frilans, versjon2),
                    forventetGradertAndreYtelse(Selvstendig, versjon3),
                    forventetGradertAndreYtelse(Arbeidsledig, versjon4),
                ),
                message.graderteAndreYtelser,
            )
        }

    @Test
    fun `Tom løsning på graderte andre ytelser gir ingen graderte andre ytelser`() =
        Toggle.GraderteAndreYtelser.enable {
            val message = checkNotNull(sendMedGraderteAndreYtelserToggle(medGraderteAndreYtelser())) { "Meldingen ble ikke gjenkjent" }
            assertEquals(emptyList<GraderteAndreYtelserForBeregning>(), message.graderteAndreYtelser)
        }

    @Test
    fun `Graderte andre ytelser for arbeidstaker uten organisasjonsnummer avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("ARBEIDSTAKER", organisasjonsnummer = null))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Graderte andre ytelser med ugyldig organisasjonsnummer avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("ARBEIDSTAKER", organisasjonsnummer = "a1"))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Graderte andre ytelser med ukjent yrkesaktivitetstype avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("JORDBRUKER", organisasjonsnummer = null))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Graderte andre ytelser uten versjon avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("SELVSTENDIG", organisasjonsnummer = null).apply { remove("versjon") })
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Graderte andre ytelser med trekk over 100 avvises`() =
        Toggle.GraderteAndreYtelser.enable {
            val json = medGraderteAndreYtelser(gradertAndreYtelse("SELVSTENDIG", organisasjonsnummer = null, trekkFraSykdomsgrad = 101))
            assertNull(sendMedGraderteAndreYtelserToggle(json))
        }

    @Test
    fun `Mapping uten løsning på avslagsdager`() {
        assertNull(sendJson(medObject).avslagsdager)
    }

    @Test
    fun `Løsning på avslagsdager ignoreres uten toggle`() {
        val json = medAvslagsdager(avslagsdag(UUID.randomUUID(), "2018-01-01", "OPPHOLD_I_NORGE_IKKE_EØS_BORGER"))
        assertNull(sendJson(json).avslagsdager)
    }

    @Test
    fun `Mapping av avslagsdager med vilkårskoder for oppholdskrav`() =
        Toggle.Avslagsdagerbehov.enable {
            val vurderingId = UUID.randomUUID()
            val json =
                medAvslagsdager(
                    avslagsdag(vurderingId, "2018-01-01", "OPPHOLD_I_NORGE_EØS_ELLER_AVTALELAND", "UNNTAK_INNLAGT_HELSEINSTITUSJON"),
                    avslagsdag(vurderingId, "2018-01-02", "OPPHOLD_I_NORGE_IKKE_EØS_BORGER"),
                    avslagsdag(vurderingId, "2018-01-03", "UNNTAK_MEDLEM_ETTER_2_5_2_6_2_8"),
                    avslagsdag(vurderingId, "2018-01-04", "UNNTAK_GODKJENT_OPPHOLD_INNTIL_FIRE_UKER"),
                )
            assertEquals(
                Avslagsdager(
                    setOf(vurderingId),
                    listOf(1.januar, 2.januar, 3.januar, 4.januar).map {
                        Avslagsdager.Dag(vurderingId, it, Begrunnelse.Oppholdskrav)
                    },
                ),
                sendJson(json).avslagsdager,
            )
        }

    @Test
    fun `Tom løsning på avslagsdager gir ingen avslagsdager`() =
        Toggle.Avslagsdagerbehov.enable {
            assertEquals(Avslagsdager(emptySet(), emptyList()), sendJson(medAvslagsdager()).avslagsdager)
        }

    @Test
    fun `Avslagsdager med ukjent vilkårskode avvises`() =
        Toggle.Avslagsdagerbehov.enable {
            val json = medAvslagsdager(avslagsdag(UUID.randomUUID(), "2018-01-01", "OPPHOLD_I_NORGE_EØS_ELLER_AVTALELAND", "ARBEIDSUFØR"))
            assertThrows<IllegalStateException> { sendJson(json) }
        }

    @Test
    fun `Avslagsdager uten vilkårskoder avvises`() =
        Toggle.Avslagsdagerbehov.enable {
            val json = medAvslagsdager(avslagsdag(UUID.randomUUID(), "2018-01-01"))
            assertThrows<IllegalStateException> { sendJson(json) }
        }

    @Test
    fun `Avslagsdager uten vurderingId avvises`() =
        Toggle.Avslagsdagerbehov.enable {
            val json = medAvslagsdager(avslagsdag(UUID.randomUUID(), "2018-01-01", "OPPHOLD_I_NORGE_IKKE_EØS_BORGER").apply { remove("vurderingId") })
            assertThrows<IllegalStateException> { sendJson(json) }
        }

    @Test
    fun `Avslagsdager uten vurderingIder i løsningen avvises`() =
        Toggle.Avslagsdagerbehov.enable {
            val json = spleisJsonMapperBuilder().build().readTree(medAvslagsdager()) as ObjectNode
            (json.path("@løsning").path("Avslagsdager") as ObjectNode).remove("vurderingIder")
            assertThrows<IllegalStateException> { sendJson(json.toString()) }
        }

    @Test
    fun `Oppfylte vurderinger uten avslagsdager mappes`() =
        Toggle.Avslagsdagerbehov.enable {
            val vurderingId = UUID.randomUUID()
            val json = medAvslagsdager(vurderingIder = setOf(vurderingId.toString()))
            assertEquals(Avslagsdager(setOf(vurderingId), emptyList()), sendJson(json).avslagsdager)
        }

    private fun avslagsdag(
        vurderingId: UUID,
        dato: String,
        vararg vilkårskoder: String,
    ) = spleisJsonMapperBuilder().build().createObjectNode().apply {
        put("vurderingId", vurderingId.toString())
        put("dato", dato)
        putArray("vilkårskoder").apply { vilkårskoder.forEach { add(it) } }
    }

    private fun medAvslagsdager(
        vararg avslagsdager: ObjectNode,
        vurderingIder: Set<String> = avslagsdager.map { it.path("vurderingId").asString() }.toSet(),
    ): String {
        val json = spleisJsonMapperBuilder().build().readTree(medArray)
        val løsning = (json.path("@løsning") as ObjectNode).putObject("Avslagsdager")
        løsning.putArray("vurderingIder").apply { vurderingIder.forEach { add(it) } }
        løsning.putArray("avslagsdager").addAll(avslagsdager.toList())
        return json.toString()
    }

    private fun gradertAndreYtelse(
        yrkesaktivitetstype: String,
        organisasjonsnummer: String?,
        versjon: UUID = UUID.randomUUID(),
        trekkFraSykdomsgrad: Int = 20,
    ) = spleisJsonMapperBuilder().build().createObjectNode().apply {
        put("yrkesaktivitetstype", yrkesaktivitetstype)
        if (organisasjonsnummer != null) put("organisasjonsnummer", organisasjonsnummer) else putNull("organisasjonsnummer")
        put("versjon", versjon.toString())
        putArray("dager").apply {
            addObject().apply {
                put("dato", "2018-01-01")
                put("trekkFraSykdomsgrad", trekkFraSykdomsgrad)
                put("sykdomsgradFørFordeling", 50)
                putObject("andreYtelserGrader").put("FORELDREPENGER", 20)
                put("sykdomsgrad", 30)
                put("arbeidsgrad", 50)
            }
            addObject().apply {
                put("dato", "2018-01-02")
                put("trekkFraSykdomsgrad", 0)
                put("sykdomsgradFørFordeling", 100)
            }
        }
    }

    private fun forventetGradertAndreYtelse(
        yrkesaktivitet: Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet,
        versjon: UUID,
    ) = GraderteAndreYtelserForBeregning(
        yrkesaktivitet = yrkesaktivitet,
        versjon = versjon,
        dager =
            listOf(
                GraderteAndreYtelserForBeregning.Dag(1.januar, trekkFraSykdomsgrad = 20, sykdomsgradFørFordeling = 50),
                GraderteAndreYtelserForBeregning.Dag(2.januar, trekkFraSykdomsgrad = 0, sykdomsgradFørFordeling = 100),
            ),
    )

    private fun medGraderteAndreYtelser(vararg graderteAndreYtelser: ObjectNode): String {
        val json = spleisJsonMapperBuilder().build().readTree(medArray)
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

    private fun medMedlemskapsvurderingResultat(
        json: String,
        svar: String,
    ): String {
        val json = spleisJsonMapperBuilder().build().readTree(json)
        (json.path("@løsning") as ObjectNode).putObject("MedlemskapsvurderingResultat").apply {
            put("svar", svar)
            put("ok", svar != "NEI")
        }
        return json.toString()
    }

    private fun medRettighetskategoriResultat(
        json: String,
        hovedkategori: String,
    ): String {
        val json = spleisJsonMapperBuilder().build().readTree(json)
        (json.path("@løsning") as ObjectNode).putObject("RettighetskategoriResultat").put("hovedkategori", hovedkategori)
        return json.toString()
    }

    private fun fjernLøsninger(
        json: String,
        vararg fjern: String,
    ): String {
        val json = spleisJsonMapperBuilder().build().readTree(json)
        val løsninger = json.path("@løsning") as ObjectNode
        løsninger.remove(fjern.toList())
        return json.toString()
    }

    private companion object {
        private val versjon1 = UUID.randomUUID()
        private val versjon2 = UUID.randomUUID()
        private val versjon3 = UUID.randomUUID()
        private val versjon4 = UUID.randomUUID()
        private val forventetPleiepenger = Pleiepenger(listOf(GradertPeriode(1.januar til 15.januar, 50)))
        private val forventetForeldrepenger = Foreldrepenger(listOf(GradertPeriode(2.januar til 16.januar, 60)))
        private val forventetSvangerskapspenger = Svangerskapspenger(listOf(GradertPeriode(1.juni til 30.juni, 99)))
        private val forventetOmsorgspenger = Omsorgspenger(listOf(GradertPeriode(3.januar til 17.januar, 70)))
        private val forventetOpplæringspenger = Opplæringspenger(listOf(GradertPeriode(4.januar til 18.januar, 80)))
        private val forventetInstitusjonsopphold =
            Institusjonsopphold(listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.januar, 31.januar)))
        private val forventetInntekterForBeregning =
            InntekterForBeregning(
                listOf(
                    InntekterForBeregning.Inntektsperiode(
                        "heihei",
                        1.april til 30.april,
                        100.daglig,
                    ),
                ),
            )
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
