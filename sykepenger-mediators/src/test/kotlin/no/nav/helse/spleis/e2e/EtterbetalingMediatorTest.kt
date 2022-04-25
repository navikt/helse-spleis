package no.nav.helse.spleis.e2e

import no.nav.helse.mai
import no.nav.helse.oktober
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test

internal class EtterbetalingMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `etterbetale med nytt grunnbeløp`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mai(2020), tom = 31.mai(2020), sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.mai(2020), tom = 31.mai(2020), sykmeldingsgrad = 100)))
        val merEnn6GInntekt = 60000.0
        sendInntektsmelding(
            listOf(Periode(fom = 1.mai(2020), tom = 16.mai(2020))),
            førsteFraværsdag = 1.mai(2020),
            beregnetInntekt = merEnn6GInntekt
        )
        sendYtelser(0)
        sendVilkårsgrunnlag(
            0,
            inntekter = sammenligningsgrunnlag(
                skjæringstidspunkt = 1.mai(2020),
                inntekter = listOf(TestMessageFactory.InntekterForSammenligningsgrunnlagFraLøsning.Inntekt(merEnn6GInntekt, ORGNUMMER))
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                skjæringstidspunkt = 1.mai(2020),
                inntekter = listOf(TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(merEnn6GInntekt, ORGNUMMER))
            )
        )
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendEtterbetaling(gyldighetsdato = 1.oktober(2020))
        sendEtterbetalingMedHistorikk(gyldighetsdato = 1.oktober(2020))
        sendUtbetaling()

        assertUtbetalingTilstander(0, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertUtbetalingTilstander(1, "IKKE_UTBETALT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALT")
        assertUtbetalingtype(0, "UTBETALING")
        assertUtbetalingtype(1, "ETTERUTBETALING")
    }

}
