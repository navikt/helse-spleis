package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvsluttetMedVedtakKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtak med utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val (inntektsmeldingId, _) = sendInntektsmelding(
            listOf(Periode(fom = 3.januar, tom = 18.januar)),
            førsteFraværsdag = 3.januar
        )
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        @Language("JSON")
        val forventet = """{
  "@event_name": "avsluttet_med_vedtak",
  "vedtakFattetTidspunkt": "<timestamp>",
  "vedtaksperiodeId": "<uuid>",
  "behandlingId": "<uuid>",
  "utbetalingId": "<uuid>",
  "fødselsnummer": "$UNG_PERSON_FNR_2018",
  "organisasjonsnummer": "$ORGNUMMER",
  "yrkesaktivitetstype": "ARBEIDSTAKER",
  "fom": "2018-01-03",
  "tom": "2018-01-26",
  "skjæringstidspunkt": "2018-01-03",
  "sykepengegrunnlag": 372000.0,
  "hendelser": [
    "$søknadId",
    "$inntektsmeldingId"
  ],
  "sykepengegrunnlagsfakta": {
    "fastsatt": "EtterHovedregel",
    "omregnetÅrsinntekt": 372000.0,
    "omregnetÅrsinntektTotalt": 372000.0,
    "sykepengegrunnlag": 372000.0,
    "6G": 561804.0,
    "arbeidsgivere": [
      {
        "arbeidsgiver": "987654321",
        "omregnetÅrsinntekt": 372000.0,
        "inntektskilde": "Arbeidsgiver"
      }
    ]
  },
  "automatiskBehandling": false,
  "forbrukteSykedager": 6,
  "gjenståendeSykedager": 242,
  "foreløpigBeregnetSluttPåSykepenger": "2019-01-01",
  "utbetalingsdager": [
    {
      "dato": "2018-01-03",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-04",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-05",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-06",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-07",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-08",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-09",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-10",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-11",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-12",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-13",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-14",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-15",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-16",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-17",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-18",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-19",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-20",
      "type": "NavHelgDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-21",
      "type": "NavHelgDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-22",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-23",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-24",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-25",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-26",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 1431,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    }
  ]
}
        """
        assertVedtakFattet(forventet, forventetUtbetalingEventNavn = "utbetaling_utbetalt")
    }

    @Test
    fun `vedtak med utbetaling hvor sykepengegrunnlaget er fastsatt ved skjønn`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val (inntektsmeldingId, _) = sendInntektsmelding(
            listOf(Periode(fom = 3.januar, tom = 18.januar)),
            førsteFraværsdag = 3.januar,
            beregnetInntekt = 45000.00
        )
        sendVilkårsgrunnlag(0)
        sendSkjønnsmessigFastsettelse(3.januar, listOf(TestMessageFactory.SkjønnsmessigFastsatt(ORGNUMMER, 47500.00 * 12)))
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK, forventedeFagområder = setOf("SP", "SPREF"))
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        @Language("JSON")
        val forventet = """{
  "@event_name": "avsluttet_med_vedtak",
  "vedtakFattetTidspunkt": "<timestamp>",
  "vedtaksperiodeId": "<uuid>",
  "behandlingId": "<uuid>",
  "utbetalingId": "<uuid>",
  "fødselsnummer": "$UNG_PERSON_FNR_2018",
  "organisasjonsnummer": "$ORGNUMMER",
  "yrkesaktivitetstype": "ARBEIDSTAKER",
  "fom": "2018-01-03",
  "tom": "2018-01-26",
  "skjæringstidspunkt": "2018-01-03",
  "sykepengegrunnlag": 561804.0,
  "hendelser": [
    "$søknadId",
    "$inntektsmeldingId"
  ],
  "sykepengegrunnlagsfakta": {
    "fastsatt": "EtterSkjønn",
    "omregnetÅrsinntekt": 540000.0,
    "omregnetÅrsinntektTotalt": 540000.0,
    "skjønnsfastsatt": 570000.0,
    "sykepengegrunnlag": 561804.0,
    "6G": 561804.0,
    "arbeidsgivere": [
      {
        "arbeidsgiver": "987654321",
        "omregnetÅrsinntekt": 540000.0,
        "skjønnsfastsatt": 570000.0,
        "inntektskilde": "Saksbehandler"
      }
    ]
  },
  "automatiskBehandling": false,
  "forbrukteSykedager": 6,
  "gjenståendeSykedager": 242,
  "foreløpigBeregnetSluttPåSykepenger": "2019-01-01",
  "utbetalingsdager": [
    {
      "dato": "2018-01-03",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-04",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-05",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-06",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-07",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-08",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-09",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-10",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-11",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-12",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-13",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-14",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-15",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-16",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-17",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-18",
      "type": "ArbeidsgiverperiodeDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-19",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-20",
      "type": "NavHelgDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-21",
      "type": "NavHelgDag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-22",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-23",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-24",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-25",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-26",
      "type": "NavDag",
      "beløpTilArbeidsgiver": 2077,
      "beløpTilBruker": 84,
      "sykdomsgrad": 100,
      "begrunnelser": null
    }
  ]
}
        """
        assertVedtakFattet(forventet, forventetUtbetalingEventNavn = "utbetaling_utbetalt")
    }

    @Test
    fun `vedtak uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(19.januar, 26.januar, FravarstypeDTO.FERIE))
        )

        @Language("JSON")
        val forventet = """
        {
            "@event_name": "avsluttet_uten_vedtak",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "yrkesaktivitetstype": "ARBEIDSTAKER",
            "fom": "2018-01-03",
            "tom": "2018-01-26",
            "skjæringstidspunkt": "2018-01-03",
            "hendelser": ["$søknadId"],
            "vedtaksperiodeId": "<uuid>",
            "behandlingId": "<uuid>",
            "avsluttetTidspunkt": "<timestamp>"
        }
        """

        testRapid.assertUtgåendeMelding(forventet)
        assertTrue(testRapid.inspektør.meldinger("avsluttet_med_vedtak").isEmpty())
        assertTrue(testRapid.inspektør.meldinger("utbetaling_utbetalt").isEmpty())
        assertTrue(testRapid.inspektør.meldinger("utbetaling_uten_utbetaling").isEmpty())
    }

    @Test
    fun `vedtak med utbetaling uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(fom = 27.januar, tom = 31.januar, FravarstypeDTO.FERIE))
        )
        sendYtelser(1)
        sendUtbetalingsgodkjenning(1)
        @Language("JSON")
        val forventet = """{
  "@event_name": "avsluttet_med_vedtak",
  "vedtakFattetTidspunkt": "<timestamp>",
  "vedtaksperiodeId": "<uuid>",
  "behandlingId": "<uuid>",
  "utbetalingId": "<uuid>",
  "fødselsnummer": "$UNG_PERSON_FNR_2018",
  "organisasjonsnummer": "$ORGNUMMER",
  "yrkesaktivitetstype": "ARBEIDSTAKER",
  "fom": "2018-01-27",
  "tom": "2018-01-31",
  "skjæringstidspunkt": "2018-01-03",
  "sykepengegrunnlag": 372000.0,
  "hendelser": [
    "$søknadId"
  ],
  "sykepengegrunnlagsfakta": {
    "fastsatt": "EtterHovedregel",
    "omregnetÅrsinntekt": 372000.0,
    "omregnetÅrsinntektTotalt": 372000.0,
    "sykepengegrunnlag": 372000.0,
    "6G": 561804.0,
    "arbeidsgivere": [
      {
        "arbeidsgiver": "987654321",
        "omregnetÅrsinntekt": 372000.0,
        "inntektskilde": "Arbeidsgiver"
      }
    ]
  },
  "automatiskBehandling": false,
  "forbrukteSykedager": 6,
  "gjenståendeSykedager": 242,
  "foreløpigBeregnetSluttPåSykepenger": "2019-01-04",
  "utbetalingsdager": [
    {
      "dato": "2018-01-27",
      "type": "Feriedag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 0,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-28",
      "type": "Feriedag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 0,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-29",
      "type": "Feriedag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 0,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-30",
      "type": "Feriedag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 0,
      "begrunnelser": null
    },
    {
      "dato": "2018-01-31",
      "type": "Feriedag",
      "beløpTilArbeidsgiver": 0,
      "beløpTilBruker": 0,
      "sykdomsgrad": 0,
      "begrunnelser": null
    }
  ]
}
        """
        assertVedtakFattet(forventet, forventetUtbetalingEventNavn = "utbetaling_uten_utbetaling")
    }

    private fun assertVedtakFattet(forventetMelding: String, forventetUtbetalingEventNavn: String) {
        val vedtakFattet = testRapid.assertUtgåendeMelding(forventetMelding)
        val vedtakFattetUtbetalingId = vedtakFattet.path("utbetalingId").asText()
        val utbetalingEventUtbetalingId = testRapid.inspektør.siste(forventetUtbetalingEventNavn).path("utbetalingId").asText()
        assertEquals(vedtakFattetUtbetalingId, utbetalingEventUtbetalingId)
    }
}


