package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.OVERFØRT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UtbetalingOgAnnulleringTest : AbstractEndToEndTest() {

    @Test
    fun `tillater ikke opprettelse av overlappende oppdrag`() {
        /**
         * før commit 5b78da0 så ble utbetalingene til vedtaksperiodene
         * gruppert på arbeidsgiver, at alle vedtaksperioder med samme AGP ble
         * utbetalt på samme fagsystemId.
         * Når en slik utbetalingsak ble revurdert så ble perioden begrenset
         * til vedtaksperioden som ble revurdert, men senere oppdragslinjer ble stiplet inn på oppdraget.
         * For eksempel hvis det var 3 vedtaksperioder (januar, februar, mars) med samme fagsystemId og
         * januar ble revurdert så ble perioden til utbetalingen begrenset til januar, mens linjene for februar og mars
         * ble kopiert inn.
         *
         * Fordi vi brukte arbeidsgiverperioden som utgangspunkt for å finne utbetalinger så ville februar og mars
         * likevel finne ut at de skulle bygge videre på samme utbetaling siden de deler arbeidsgiverperiode med januar.
         *
         * ETTER commit 5b78da0 så blir saken litt annerledes: da blir det opprettet en utbetalingsak for hver
         * vedtaksperiode. Det betyr at når en vedtaksperiode skulle finne riktig utbetaling å bygge videre på så
         * brukte vi kun 'vedtaksperiodens periode' til å finne overlapp.
         * I eksempelet over ville da februar og mars plutselig ikke finne noen tidligere utbetaling lenger,
         * siden ingen utbetalinger hadde en 'periode' som overlappet med februar eller mars. Til tross for at
         * utbetalingen hadde oppdragslinjer som strakk seg over februar og mars.
         */
        createTestPerson { regelverkslogg ->
            gjenopprettFraJSON("/personer/revurdering-av-flere-vedtak-i-samme-utbetaling.json", regelverkslogg)
        }

        this@UtbetalingOgAnnulleringTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        val m = assertThrows<IllegalStateException> {
            this@UtbetalingOgAnnulleringTest.håndterYtelser(2.vedtaksperiode)
        }
        assertEquals("Har laget en overlappende utbetaling", m.message)
    }

    @Test
    fun `utbetaling hopper ut av fagsystemid`() {
        createPersonMedToVedtakPåSammeFagsystemId()
        this@UtbetalingOgAnnulleringTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.februar, Dagtype.Feriedag)))
        this@UtbetalingOgAnnulleringTest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        val utbetaling = inspektør.utbetaling(2)
        assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
        assertEquals(19.januar til 19.februar, utbetaling.arbeidsgiverOppdrag[0].periode)
        assertEquals(3.januar til 20.februar, utbetaling.periode)
    }

    @Test
    fun `annullere senere periode enn perioden til godkjenning`() {
        nyttVedtak(mars)
        tilGodkjenning(januar, a1, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_ANNULLERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    @Test
    fun `annullere førstegangsbehandling uten utbetaling`() {
        nyPeriode(1.januar til 20.januar, grad = 19.prosent)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())

        håndterAnnullerUtbetaling(a1)
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetaling(1).tilstand)
    }

    @Test
    fun `periode med syk nav strekkes tilbake med foreldrepenger - opphører tidligere utbetaling`() {
        håndterSøknad(1.februar til 2.februar)
        håndterInntektsmelding(
            emptyList(),
            førsteFraværsdag = 1.februar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening"
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)))
        assertVarsler(listOf(Varselkode.RV_AY_5, Varselkode.RV_AY_12, RV_IM_8), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        this@UtbetalingOgAnnulleringTest.håndterOverstyrTidslinje(
            (januar).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) } +
                listOf(ManuellOverskrivingDag(1.februar, Dagtype.Sykedag, 100))
        )
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)))
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val utbetaling = inspektør.utbetaling(0)
        val revurdering = inspektør.utbetaling(1)
        assertEquals(utbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
        revurdering.arbeidsgiverOppdrag[0].inspektør.also { linje ->
            assertEquals(1.februar til 1.februar, linje.fom til linje.tom)
            assertEquals(Endringskode.ENDR, linje.endringskode)
            assertEquals("OPPH", linje.statuskode)
            assertEquals(1.februar, linje.datoStatusFom)
            assertEquals(1, linje.delytelseId)
            Assertions.assertNull(linje.refDelytelseId)
        }
    }

    @Test
    fun `annullerer første periode før andre periode starter i en ikke-sammenhengende utbetaling med mer enn 16 dager gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        val utbetalingId = inspektør.sisteUtbetaling().utbetalingId
        håndterAnnullerUtbetaling(utbetalingId = utbetalingId)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(20.februar, 20.mars))
        håndterSøknad(20.februar til 20.mars)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(20.februar, 7.mars)),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(26.januar, observatør.annulleringer[0].tom)
        assertEquals(20.mars, observatør.utbetalingMedUtbetalingEventer.last().tom)
    }

    @Test
    fun `kan annullere selv om vi har en etterfølgende periode som har gått til infotrygd etter simulering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(3.mars, 26.mars))
        håndterSøknad(3.mars til 26.mars)
        håndterArbeidsgiveropplysninger(
            listOf(Periode(3.mars, 18.mars)),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
        assertEquals(3, inspektør.antallUtbetalinger)
        assertTrue(inspektør.utbetaling(2).erAnnullering)
    }

    @Test
    fun `kan ikke annullere utbetaling etter sammenhengede periode TIL_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 15.februar))
        håndterSøknad(27.januar til 15.februar)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
        assertEquals(2, inspektør.antallUtbetalinger)
        assertFalse(inspektør.utbetaling(1).erAnnullering)
    }

    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling med teknisk feil blir stående i til utbetaling og prøver igjen`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, OVERFØRT, LocalDateTime.now().minusDays(1))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling med teknisk feil for lenge går til Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.FEIL, sendOverførtKvittering = false)
        håndterUtbetalingpåminnelse(0, OVERFØRT, LocalDateTime.now().minusDays(8))
        this@UtbetalingOgAnnulleringTest.håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `utbetaling som blir avvist går til utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AVVIST)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Test
    fun `kan forsøke utbetaling på nytt etter Utbetaling feilet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AVVIST)
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
        håndterUtbetalingpåminnelse(0, OVERFØRT)
        assertEquals(OVERFØRT, inspektør.utbetalingtilstand(0))
        håndterUtbetalt(Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `utbetaling går videre dersom vi går glipp av Overført-kvittering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT, sendOverførtKvittering = false)
        assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetalingtilstand(0))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Kan ikke annullere over en fastlåst annullering`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode)) // Stale
        håndterAnnullerUtbetaling(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode), opprettet = LocalDateTime.now().plusHours(3))
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)

        assertTrue(inspektør.sisteUtbetaling().erAnnullering)
        assertEquals(1, observatør.annulleringer.size)
        assertEquals(
            2,
            personlogg.behov
                .filter { it.detaljer()["fagsystemId"] == inspektør.sisteArbeidsgiveroppdragFagsystemId(1.vedtaksperiode) }
                .filter { it.type == Aktivitet.Behov.Behovtype.Utbetaling }
                .size
        )
        assertEquals(2, inspektør.antallUtbetalinger)
    }

    @Test
    fun `utbetaling med ubetalt periode etterpå inkluderer ikke dager etter perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(3.januar til 26.januar)
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        val utbetalingUtbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
        assertEquals(3.januar til 26.januar, utbetalingUtbetalingstidslinje.periode())
    }

    @Test
    fun `utbetaling_utbetalt tar med begrunnelse på avviste dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), Sykmeldingsperiode(21.januar, 30.januar))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Sykdom(21.januar, 30.januar, 15.prosent))
        håndterArbeidsgiveropplysninger(listOf(Periode(1.januar, 16.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@UtbetalingOgAnnulleringTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())

        val avvisteDager = observatør.utbetalingMedUtbetalingEventer.first().utbetalingsdager.filter { it.type == PersonObserver.Utbetalingsdag.Dagtype.AvvistDag }
        val ikkeAvvisteDager = observatør.utbetalingMedUtbetalingEventer.first().utbetalingsdager.filter { it.type != PersonObserver.Utbetalingsdag.Dagtype.AvvistDag }

        assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
        assertEquals(7, avvisteDager.size)
        assertEquals(23, ikkeAvvisteDager.size)
        assertTrue(avvisteDager.all { it.begrunnelser == listOf(PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO.MinimumSykdomsgrad) })
        assertTrue(ikkeAvvisteDager.all { it.begrunnelser == null })
    }
}
