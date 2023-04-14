package no.nav.helse.spleis.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.KontraktAssertions.assertUtgåendeMelding
import no.nav.helse.spleis.e2e.KontraktAssertions.assertOgFjernLocalDateTime
import no.nav.helse.spleis.e2e.KontraktAssertions.assertOgFjernUUID
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtakkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtak med utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val (inntektsmeldingId,_) = sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        @Language("JSON")
        val forventet = """
        {
            "@event_name": "vedtak_fattet",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom": "2018-01-03",
            "tom": "2018-01-26",
            "skjæringstidspunkt": "2018-01-03",
            "sykepengegrunnlag": 372000.0,
            "grunnlagForSykepengegrunnlag": 372000.0,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver": {
                "987654321": 372000.0
            },
            "inntekt" : 31000.0,
            "begrensning" : "ER_IKKE_6G_BEGRENSET",
            "hendelser": ["$søknadId", "$inntektsmeldingId"]
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
            "@event_name": "vedtak_fattet",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom": "2018-01-03",
            "tom": "2018-01-26",
            "skjæringstidspunkt": "2018-01-03",
            "sykepengegrunnlag": 0.0,
            "grunnlagForSykepengegrunnlag": 0.0,
            "inntekt" : 0.0,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver": {},
            "hendelser": ["$søknadId"],
            "begrensning" : "VET_IKKE"
        }
        """
        assertVedtakFattet(forventet, forventetUtbetalingEventNavn = null)
    }

    @Test
    fun `vedtak med utbetaling uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val (inntektsmeldingId, _) = sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
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
        val forventet = """
        {
            "@event_name": "vedtak_fattet",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom" : "2018-01-27",
            "tom" : "2018-01-31",
            "skjæringstidspunkt": "2018-01-03",
            "sykepengegrunnlag": 372000.0,
            "grunnlagForSykepengegrunnlag": 372000.0,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver": {
                "987654321": 372000.0
            },
            "inntekt" : 31000.0,
            "begrensning" : "ER_IKKE_6G_BEGRENSET",
            "hendelser": ["$søknadId", "$inntektsmeldingId"]
        }
        """
        assertVedtakFattet(forventet, forventetUtbetalingEventNavn = "utbetaling_uten_utbetaling")
    }

    private fun assertVedtakFattet(forventetMelding: String, forventetUtbetalingEventNavn: String?) {
        val vedtakFattet = testRapid.assertUtgåendeMelding(forventetMelding) {
            it.assertOgFjernUUID("vedtaksperiodeId")
            it.assertOgFjernLocalDateTime("vedtakFattetTidspunkt")
            if (forventetUtbetalingEventNavn != null) it.assertOgFjernUUID("utbetalingId")
        }

        if (forventetUtbetalingEventNavn != null) {
            val vedtakFattetUtbetalingId = vedtakFattet.path("utbetalingId").asText()
            val utbetalingEventUtbetalingId = testRapid.inspektør.siste(forventetUtbetalingEventNavn).path("utbetalingId").asText()
            assertEquals(vedtakFattetUtbetalingId, utbetalingEventUtbetalingId)
        } else {
            assertTrue(testRapid.inspektør.meldinger("utbetaling_utbetalt").isEmpty())
            assertTrue(testRapid.inspektør.meldinger("utbetaling_uten_utbetaling").isEmpty())
        }
    }
}


