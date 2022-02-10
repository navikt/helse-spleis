package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvsluttetUtenUtbetalingE2ETest: AbstractEndToEndTest() {
    /*
        Hvis vi har en kort periode som har endt opp i AVSLUTTET_UTEN_UTBETALING vil alle etterkommende perioder
        bli stuck i en variant av *_UFERDIG_GAP. Da vil de aldri komme seg videre og til slutt time ut
    */
    @Test
    fun `kort periode blokkerer neste periode i ny arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(3.mars, 26.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.mars, 18.mars)))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(3.mars, 26.mars, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    /*
        Denne testen er en slags følgefeil av testen over. Det at periode #2 er kort og får inntektsmeldingen lurer oss ut av UFERDIG-løpet og lar oss
        fortsette behandling. Dessverre setter vi oss fast i AVVENTER_HISTORIKK fordi periode #1 blokkerer utførelsen i Vedtaksperiode.forsøkUtbetaling(..)
     */
    @Test
    fun `kort periode setter senere periode fast i AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(3.mars, 7.mars, 100.prosent))
        håndterSøknad(Sykdom(3.mars, 7.mars, 100.prosent))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        håndterSykmelding(Sykmeldingsperiode(8.mars, 26.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(3.vedtaksperiode, arbeidsgiverperioder = listOf(Periode(3.mars, 18.mars)))

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)

        håndterSøknadMedValidering(3.vedtaksperiode, Sykdom(8.mars, 26.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Sender vedtaksperiode_endret når inntektsmelidng kommer i AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2021), 1.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2021), 1.januar(2021), 100.prosent))

        assertEquals(2, observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).size)

        val hendelseId = håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021))

        assertEquals(3, observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).size)
        assertTrue(hendelseId in observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Vedtaksperioder i AVSLUTTET_UTEN_UTBETALING som treffes av en inntektsmelding slik at den får utebetaling - skal gå videre til AVVENTER_HISTORIKK`() = Toggle.GjenopptaAvsluttetUtenUtbetaling.enable {
        håndterSykmelding(Sykmeldingsperiode(9.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 20.januar, 100.prosent))
        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ny inntektsmelding med første fraværsdag i en sammenhengende periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        val imId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 23.januar)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(imId, 2.vedtaksperiode.id(ORGNUMMER))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((1.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
    }
}
