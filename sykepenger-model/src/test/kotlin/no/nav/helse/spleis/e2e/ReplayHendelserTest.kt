package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReplayHendelserTest : AbstractEndToEndTest() {
    @BeforeEach
    fun prepare() {
        Toggles.ReplayEnabled.enabled = true
    }

    @AfterEach
    fun tearDown() {
        Toggles.ReplayEnabled.enabled = false
    }

    @Test
    fun `Replay av etterfølgende skjer umiddelbart når ny sykmelding kommer inn`() {
        håndterSykmelding(Sykmeldingsperiode(28.januar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 25.januar, 100))
        assertAntallReplays(1)
    }

    @Test
    fun `Denne og etterfølgende perioder settes i tilstand TilInfotrygd ved forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `Støtter ikke replay for flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar, 100), orgnummer = "ANNET ORGNUMMER")
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `Denne og etterfølgende perioder settes i tilstand TilInfotrygd ved mer enn 16 dagers gap (ny arbeidsgiverperiode) da det ikke støttes ennå`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `ny, tidligere sykmelding medfører replay av etterfølgende perioder når det er gap mindre enn 16 dager`() {
        val opprinnelig = håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 27.februar, 100))
        replaySykmelding(opprinnelig)

        assertReplayAv(1.vedtaksperiode)
        assertAntallReplays(1)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `Korrekte tidslinjer ved replay av utbetalt vedtaksperiode`() {
        val sykmeldingId = håndterSykmelding(Sykmeldingsperiode(28.januar, 28.februar, 100))
        val søknadId = håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.januar, 28.februar, 100))
        val inntektsmeldingId = håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 28.januar,
            refusjon = Triple(null, 30000.månedlig, emptyList())
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        replaySykmelding(sykmeldingId)
        replaySøknad(søknadId)
        replayInntektsmelding(inntektsmeldingId)

        assertAntallReplays(1)
        assertReplayAv(1.vedtaksperiode)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar,
            refusjon = Triple(null, 29000.månedlig, emptyList())
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterYtelser(3.vedtaksperiode)

        inspektør.also {
            assertNoErrors(it)
            TestTidslinjeInspektør(it.utbetalingstidslinjer(2.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[ArbeidsgiverperiodeDag::class])
                assertEquals(3, tidslinjeInspektør.dagtelling[NavDag::class])
                assertEquals(2, tidslinjeInspektør.dagtelling[NavHelgDag::class])
            }
            // The rejected period
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[ArbeidsgiverperiodeDag::class])
                assertEquals(23, tidslinjeInspektør.dagtelling[NavDag::class])
                assertEquals(9, tidslinjeInspektør.dagtelling[NavHelgDag::class])
                assertEquals(3, tidslinjeInspektør.dagtelling[Fridag::class])
                assertEquals(8, tidslinjeInspektør.dagtelling[Arbeidsdag::class])
            }
            // The replacement period
            TestTidslinjeInspektør(it.utbetalingstidslinjer(3.vedtaksperiode)).also { tidslinjeInspektør ->
                assertEquals(null, tidslinjeInspektør.dagtelling[ArbeidsgiverperiodeDag::class])
                assertEquals(23, tidslinjeInspektør.dagtelling[NavDag::class])
                assertEquals(8, tidslinjeInspektør.dagtelling[NavHelgDag::class])
                assertEquals(1, tidslinjeInspektør.dagtelling[Fridag::class])
                assertEquals(null, tidslinjeInspektør.dagtelling[Arbeidsdag::class])
            }
        }
    }
}
