package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.*
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class FeriepengeMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode med en utbetaling i infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 1.juni(2020), tom = 30.juni(2020), sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.juni(2020), tom = 16.juni(2020))), førsteFraværsdag = 1.juni(2020))
        sendYtelser(0)
        sendVilkårsgrunnlag(0, (1..12).map { YearMonth.of(2019, it).plusMonths(5) to INNTEKT })
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendUtbetalingshistorikkForFeriepenger(
            TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata(
                fom = 1.januar(2020),
                tom = 31.desember(2020),
                utbetalinger = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Utbetaling(
                        fom = 1.mars(2020),
                        tom = 31.mars(2020),
                        dagsats = 1431.0,
                        typekode = "5",
                        utbetalingsgrad = "100",
                        organisasjonsnummer = ORGNUMMER
                    )
                ),
                feriepengehistorikk = listOf(
                    TestMessageFactory.UtbetalingshistorikkForFeriepengerTestdata.Feriepenger(
                        orgnummer = ORGNUMMER,
                        beløp = 3211,
                        fom = 1.mai(2021),
                        tom = 31.mai(2021)
                    )
                )
            )
        )
    }
}
