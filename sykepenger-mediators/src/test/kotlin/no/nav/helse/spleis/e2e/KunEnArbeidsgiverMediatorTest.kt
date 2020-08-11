package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class KunEnArbeidsgiverMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `påminnelse for vedtaksperiode som ikke finnes`() {
        sendNyPåminnelse()
        assertEquals(1, testRapid.inspektør.antall())
        assertEquals("vedtaksperiode_ikke_funnet", testRapid.inspektør.melding(0).path("@event_name").asText())
    }

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling(0)

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )
    }

    @Test
    fun `perioder påvirket av "kanseller utbetaling"-event går til Infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling(0)
        sendKansellerUtbetaling()
        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET",
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Ny, tidligere sykmelding medfører replay av første periode`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))

        assertForkastedeTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertIkkeForkastedeTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertIkkeForkastedeTilstander(2, "MOTTATT_SYKMELDING_UFERDIG_GAP")
    }

    @Test
    fun `Inntektsmelding med første fraværsdag 1 januar skal ikke gjøre at sykmelding nr 2 ikke blir behandlet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(0, perioder = listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 28.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 2.februar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        assertForkastedeTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP")
        assertIkkeForkastedeTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_SØKNAD_FERDIG_GAP")
        assertIkkeForkastedeTilstander(2, "MOTTATT_SYKMELDING_UFERDIG_GAP", "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP", "AVVENTER_UFERDIG_GAP")
    }

    @Test
    fun `overstyring fra saksbehandler fører til tilstandsendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))

        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_HISTORIKK"
        )

        val sisteMelding = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        assertTrue(sisteMelding.hasNonNull("vedtaksperiodeId"))
    }
}


