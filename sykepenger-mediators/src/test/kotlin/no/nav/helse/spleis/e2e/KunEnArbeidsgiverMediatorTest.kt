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
    fun `enkel case`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(0, perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.februar, tom = 15.februar)), førsteFraværsdag = 1.februar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        assertEquals("vedtaksperiode_forkastet", testRapid.inspektør.melding(5).path("@event_name").asText())
        assertReplays(
            listOf(
                Pair("ny_søknad", 7),
                Pair("sendt_søknad_nav", 8),
                Pair("sendt_søknad_nav", 9),
                Pair("inntektsmelding", 10)
            )
        )
        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP")
        assertTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertTilstander(2, "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE", "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE", "AVVENTER_UFERDIG_FORLENGELSE")
    }

    @Disabled("Inntektsmelding med første fraværsdag gjør at sykmelding nr.2 anses som del av første, og behandling av denne blir dermed ignorert")
    @Test
    fun `Inntektsmelding med første fraværsdag 1 januar skal ikke gjøre at sykmelding nr 2 ikke blir behandlet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(0, perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 1.februar, tom = 15.februar)), førsteFraværsdag = 1.januar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        assertReplays(
            listOf(
                Pair("ny_søknad", testRapid.inspektør.antall() - 3),
                Pair("sendt_søknad_nav", testRapid.inspektør.antall() - 2),
                Pair("inntektsmelding", testRapid.inspektør.antall() - 1)
            )
        )
        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP")
        assertTilstander(1, "MOTTATT_SYKMELDING_FERDIG_GAP")
        assertTilstander(
            2,
            "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
            "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
            "AVVENTER_UFERDIG_FORLENGELSE"
        )
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


