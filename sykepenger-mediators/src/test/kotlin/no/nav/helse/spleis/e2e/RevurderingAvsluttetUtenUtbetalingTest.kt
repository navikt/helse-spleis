package no.nav.helse.spleis.e2e

import no.nav.helse.januar
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test

internal class RevurderingAvsluttetUtenUtbetalingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `revurdering ved inntektsmelding for korte perioder`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 5.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 5.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        sendNySøknad(SoknadsperiodeDTO(fom = 6.januar, tom = 10.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 6.januar, tom = 10.januar, sykmeldingsgrad = 100)))
        sendNySøknad(SoknadsperiodeDTO(fom = 11.januar, tom = 17.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 17.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelserUtenSykepengehistorikk(2)
        sendVilkårsgrunnlag(2)
        sendYtelserUtenSykepengehistorikk(2)
        sendSimulering(2, SimuleringMessage.Simuleringstatus.OK)

        assertTilstander(0, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(1, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(
            2,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING",
            "AVVENTER_GJENNOMFØRT_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_VILKÅRSPRØVING_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_SIMULERING_REVURDERING",
            "AVVENTER_GODKJENNING_REVURDERING"
        )
    }

    @Test
    fun `revurdering ved inntektsmelding for korte perioder - endring av skjæringstidspunkt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 8.januar, tom = 10.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 8.januar, tom = 10.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        sendNySøknad(SoknadsperiodeDTO(fom = 11.januar, tom = 22.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 22.januar, sykmeldingsgrad = 100)))
        sendNySøknad(SoknadsperiodeDTO(fom = 23.januar, tom = 23.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 23.januar, tom = 23.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(
            Periode(fom = 1.januar, tom = 6.januar),
            Periode(fom = 11.januar, tom = 21.januar),
        ), førsteFraværsdag = 11.januar)
        sendYtelserUtenSykepengehistorikk(2)
        sendVilkårsgrunnlag(2)
        sendYtelserUtenSykepengehistorikk(2)
        sendSimulering(2, SimuleringMessage.Simuleringstatus.OK)

        assertTilstander(0, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(1, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVVENTER_GJENNOMFØRT_REVURDERING")
        assertTilstander(
            2,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING",
            "AVVENTER_GJENNOMFØRT_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_VILKÅRSPRØVING_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_SIMULERING_REVURDERING",
            "AVVENTER_GODKJENNING_REVURDERING"
        )
    }
}
