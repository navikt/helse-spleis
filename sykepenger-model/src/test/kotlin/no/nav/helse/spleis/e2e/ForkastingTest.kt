package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class ForkastingTest : AbstractEndToEndTest() {

    //FIXME: Fjernes når alle Doubles byttes ut med Inntekt
    private val INNTEKT_AS_INT = 31000

    private val a2 = "arbeidsgiver 2"
    private val a2Inspektør get() = TestArbeidsgiverInspektør(person, a2)

    @Test
    fun `forlengelse av infotrygd uten inntektsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 23.februar, 100))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT_AS_INT, 100, ORGNUMMER),
            inntektshistorikk = emptyList()
        )
        håndterYtelser(
            1.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT_AS_INT, 100, ORGNUMMER),
            inntektshistorikk = emptyList()
        )
        assertTrue(inspektør.utbetalinger.isEmpty())
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `når utbetaling er ikke godkjent skal påfølgende perioder også kastes ut`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(29.januar, 23.februar, 100))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
        assertEquals(Utbetaling.IkkeGodkjent, inspektør.utbetalingtilstand(0))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD
        )
    }

    @Test
    fun `kan ikke forlenge en periode som er gått TilInfotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(29.januar, 23.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false) // går til TilInfotrygd

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Sykmelding i omvendt rekkefølge kaster ut etterfølgende som ikke er avsluttet`() {
        Toggles.ReplayEnabled.enable {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100))
            håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP
            )
            assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        }
    }

    @Test
    fun `Sykmelding i omvendt rekkefølge kaster ut etterfølgende som ikke er avsluttet — uten replay`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `søknad med papirsykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(21.januar, 28.februar, 100))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100),
            Søknad.Søknadsperiode.Papirsykmelding(1.januar, 20.januar)
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `refusjon opphører i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), refusjon = Triple(
                14.januar,
                INNTEKT, emptyList()
            )
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `refusjon endres i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), refusjon = Triple(
                null,
                INNTEKT, listOf(14.januar)
            )
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `søknad med utenlandsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar, 100))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Søknad.Søknadsperiode.Sykdom(2.januar, 25.januar, 100),
            Søknad.Søknadsperiode.Utlandsopphold(22.januar, 25.januar)
        )

        assertForkastetPeriodeTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Håndterer ny sykmelding som ligger tidligere i tid *med forlengelse*`() {
        håndterSykmelding(Sykmeldingsperiode(23.mars(2020), 29.mars(2020), 100))
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 2.april(2020), 100))
        håndterSykmelding(Sykmeldingsperiode(10.april(2020), 20.april(2020), 100))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)

        håndterSykmelding(Sykmeldingsperiode(19.mars(2020), 22.mars(2020), 100))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(4.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `Håndterer ny sykmelding som ligger tidligere i tid *uten forlengelse*`() {
        Toggles.ReplayEnabled.enable {
            håndterSykmelding(Sykmeldingsperiode(24.mars(2020), 29.mars(2020), 100))
            håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 2.april(2020), 100))
            håndterSykmelding(Sykmeldingsperiode(10.april(2020), 20.april(2020), 100))
            håndterSykmelding(Sykmeldingsperiode(19.mars(2020), 22.mars(2020), 100))
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP
            )
            assertForkastetPeriodeTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
            )
            assertForkastetPeriodeTilstander(
                3.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_UFERDIG_GAP
            )
            assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
            assertReplayAv(1.vedtaksperiode, 2.vedtaksperiode, 3.vedtaksperiode)
        }
    }

    @Test
    fun `invaliderer perioder når det kommer sykmelding på en arbeidsgiver som hadde tom sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    a2 inntekt 1
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100), orgnummer = a2)
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            TIL_INFOTRYGD
        )
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertEquals(0, a2Inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `forkaster ikke påfølgende periode når tilstøtende forkastet periode ble avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(29.august(2020), 25.september(2020), 100))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Søknad.Søknadsperiode.Sykdom(29.august(2020), 25.september(2020), 100)
        )
        håndterInntektsmelding(listOf(29.august(2020) til 13.september(2020)))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(29.september(2020), 30.september(2020), 100))
        håndterSøknadMedValidering(
            2.vedtaksperiode,
            Søknad.Søknadsperiode.Sykdom(29.september(2020), 30.september(2020), 100)
        )
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_FERDIG_GAP, LocalDateTime.now().minusYears(1))

        håndterSykmelding(Sykmeldingsperiode(26.september(2020), 23.oktober(2020), 100))

        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertFalse(inspektør.periodeErForkastet(3.vedtaksperiode))
    }

    @Test
    fun `forkaster ikke påfølgende periode når den forkastede ikke var avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, 55555.månedlig)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }
}
