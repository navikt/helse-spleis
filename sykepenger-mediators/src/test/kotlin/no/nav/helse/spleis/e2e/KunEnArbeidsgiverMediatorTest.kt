package no.nav.helse.spleis.e2e

import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.PeriodeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        sendInnteksmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0)
        sendManuellSaksbehandling(0)
        sendUtbetaling(0)

        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP", "AVVENTER_HISTORIKK", "AVVENTER_SIMULERING", "AVVENTER_GODKJENNING", "TIL_UTBETALING", "AVSLUTTET")
    }

    @Test
    fun `kansellerutbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInnteksmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendKansellerUtbetaling()

        assertTrue(testRapid.inspektør.behovtypeSisteMelding(Behovtype.Utbetaling))
    }

    @Test
    internal fun `overlapp i arbeidsgivertidslinjer`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 7.januar(2020), tom = 13.januar(2020), sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 14.januar(2020), tom = 24.januar(2020), sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 14.januar(2020), tom = 24.januar(2020), sykmeldingsgrad = 100)), listOf(PeriodeDTO(6.januar(2020), 6.januar(2020))))
        sendNySøknad(SoknadsperiodeDTO(fom = 25.januar(2020), tom = 7.februar(2020), sykmeldingsgrad = 80))
        sendNySøknad(SoknadsperiodeDTO(fom = 8.februar(2020), tom = 28.februar(2020), sykmeldingsgrad = 80))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 25.januar(2020), tom = 7.februar(2020), sykmeldingsgrad = 80)))
        sendInnteksmelding(0, listOf(Periode(fom = 6.januar(2020), tom = 21.januar(2020))), førsteFraværsdag = 6.januar(2020))
        sendNySøknad(SoknadsperiodeDTO(fom = 29.februar(2020), tom = 11.mars(2020), sykmeldingsgrad = 80))

        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)

        assertEquals(5, testRapid.inspektør.vedtaksperiodeteller)
        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP", "AVVENTER_HISTORIKK", "AVSLUTTET")
        assertTilstander(1, "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE", "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE", "AVVENTER_UFERDIG_FORLENGELSE", "AVVENTER_HISTORIKK")
        assertTilstander(2, "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE", "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE")
        assertTilstander(3, "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE")
        assertTilstander(4, "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE")
    }
}
