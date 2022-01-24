package no.nav.helse.spleis.e2e

import no.nav.helse.mai
import no.nav.helse.oktober
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class EtterbetalingMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `etterbetale med nytt grunnbeløp`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mai(2020), tom = 31.mai(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.mai(2020), tom = 31.mai(2020), sykmeldingsgrad = 100)))
        val merEnn6GInntekt = 60000.0
        sendInntektsmelding(0, listOf(Periode(fom = 1.mai(2020), tom = 16.mai(2020))), førsteFraværsdag = 1.mai(2020), beregnetInntekt = merEnn6GInntekt)
        sendYtelser(0)
        sendVilkårsgrunnlag(0,
            inntekter = (5.rangeTo(12).map { YearMonth.of(2019, it) to merEnn6GInntekt } + 1.rangeTo(4).map { YearMonth.of(2020, it) to merEnn6GInntekt })
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
