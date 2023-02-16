package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterOverstyringSykedag
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellArbeidsgiverdag
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellPermisjonsdag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass

internal class OverstyrTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer skjæringstidspunktet`() {
        tilGodkjenning(10.januar, 31.januar, a1)
        val vilkårsgrunnlagFørEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)

        assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Sykedag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntekt = 20000.månedlig)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        val vilkårsgrunnlagEtterEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!

        assertTidsnærInntektsopplysning(a1, vilkårsgrunnlagFørEndring.inspektør.sykepengegrunnlag, vilkårsgrunnlagEtterEndring.inspektør.sykepengegrunnlag)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer ikke skjæringstidspunktet`() {
        tilGodkjenning(10.januar, 31.januar, a1)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
        ), orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `strekker ikke inn i forrige periode`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        tilGodkjenning(10.januar, 31.januar, a1) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
        nullstillTilstandsendringer()
        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        assertEquals(4, inspektør.sykdomshistorikk.inspektør.elementer())
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
        ), orgnummer = a1)
        assertEquals(5, inspektør.sykdomshistorikk.inspektør.elementer())
        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `endrer skjæringstidspunkt på en førstegangsbehandling ved å omgjøre en arbeidsdag til sykedag`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        tilGodkjenning(10.januar, 31.januar, a1) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
        val sykepengegrunnlagFør = inspektør(a1).vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        nullstillTilstandsendringer()
        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1, inntekt = 20000.månedlig)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
        val sykepengegrunnlagEtter = inspektør(a1).vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `endrer skjæringstidspunkt på sykefraværstilfelle etter - endrer ikke dato for inntekter`() {
        håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent), Arbeid(1.januar, 9.januar), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(15.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(15.januar til 29.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(10.januar til 25.januar), orgnummer = a2)

        val inntektsvurdering = Inntektsvurdering(
            inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            })
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = inntektsvurdering, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()

        val sykepengegrunnlagFør = inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = inntektsvurdering, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)

        val sykepengegrunnlagEtter = inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)
        assertTidsnærInntektsopplysning(a2, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertEquals(1.januar til 9.januar, inspektør(a1).periode(1.vedtaksperiode))
        assertEquals(15.januar til 31.januar, inspektør(a1).periode(2.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør(a2).periode(1.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, orgnummer = a2)
    }

    private fun assertTidsnærInntektsopplysning(orgnummer: String, sykepengegrunnlagFør: Sykepengegrunnlag, sykepengegrunnlagEtter: Sykepengegrunnlag) {
        val inntektsopplysningerFørEndring = sykepengegrunnlagFør.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)
        val inntektsopplysningerEtterEndring = sykepengegrunnlagEtter.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)

        assertEquals(inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.hendelseId, inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.hendelseId)
        assertEquals(inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.beløp, inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.beløp)
        assertEquals(inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.tidsstempel, inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.tidsstempel)
        assertEquals(Inntektsmelding::class, inntektsopplysningerEtterEndring.inspektør.inntektsopplysning::class)
    }

    @Test
    fun `vedtaksperiode strekker seg ikke tilbake hvis det er en periode foran`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(10.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
        ), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Sykedag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem`() {
        nyPeriode(1.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
            ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
        ), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        assertUtbetalingsdag(18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, INGEN, INGEN)
        assertUtbetalingsdag(19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, INGEN)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem etter utbetalt`() {
        nyPeriode(1.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
            ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
        ), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        assertUtbetalingsdag(18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, INGEN, INGEN)
        assertUtbetalingsdag(19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, INGEN)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(1).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 16 dager etter utbetalt`() {
        nyPeriode(1.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dag ->
            ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
        }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertEquals(17.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(1).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)

        assertEquals(1.januar til 31.januar, revurdering.periode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt`() {
        nyPeriode(1.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje((1.januar til 10.januar).map { dag ->
            ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
        }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
        assertEquals(11.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(1).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(1.januar til 31.januar, revurdering.periode)
        revurdering.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(2, oppdrag.size)
            oppdrag[0].inspektør.also { linje ->
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(17.januar, linje.datoStatusFom)
                assertEquals(Endringskode.ENDR, linje.endringskode)
                assertEquals("OPPH", linje.statuskode)
            }
            oppdrag[1].inspektør.also { linje ->
                assertEquals(27.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(Endringskode.NY, linje.endringskode)
            }
        }
        assertEquals(0, revurdering.personOppdrag.size)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }
    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt - med tidligere utbetalt vedtak`() {
        nyttVedtak(1.januar, 31.januar)

        nyPeriode(1.mars til 31.mars, a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje((1.mars til 10.mars).map { dag ->
            ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
        }, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

        assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

        assertEquals(11.mars, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(1.mars til 31.mars, inspektør.periode(2.vedtaksperiode))

        val førsteUtbetaling = inspektør.utbetaling(1).inspektør
        val revurdering = inspektør.utbetaling(2).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(1.mars til 31.mars, revurdering.periode)
        assertEquals(1.januar til 31.mars, revurdering.utbetalingstidslinje.periode())
        revurdering.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(2, oppdrag.size)
            oppdrag[0].inspektør.also { linje ->
                assertEquals(17.mars til 30.mars, linje.fom til linje.tom)
                assertEquals(17.mars, linje.datoStatusFom)
                assertEquals(Endringskode.ENDR, linje.endringskode)
                assertEquals("OPPH", linje.statuskode)
            }
            oppdrag[1].inspektør.also { linje ->
                assertEquals(27.mars til 30.mars, linje.fom til linje.tom)
                assertEquals(Endringskode.NY, linje.endringskode)
            }
        }
        assertEquals(0, revurdering.personOppdrag.size)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    @Disabled("må skrive kode for å kaste exception; eller gjøre slik at vi starter revurdering før")
    fun `innfører ny arbeidsgiverperiode på en førstegangsbehandling etter tidligere utbetalt`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(14.februar, 10.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()
        val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        håndterOverstyrTidslinje((14.februar til 16.februar).map { dag ->
            ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
        }, orgnummer = a1)

        assertForventetFeil(
            forklaring = "ved å innføre arbeidsdager/ny AGP for februar så påvirkes egentlig utbetalingen som er påstartet i januar," +
                    "fordi utbetalingen i januar skal ikke lengre utbetale februar. februar skal utbetales på en egen fagsystemId/oppdrag." +
                    "Derfor må overstyr tidslinje-hendelsen igangsette revurdering slik at januar blir med, og revurderes først",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

                håndterYtelser(2.vedtaksperiode)
                håndterVilkårsgrunnlag(2.vedtaksperiode)
                assertThrows<IllegalStateException> {
                    håndterYtelser(2.vedtaksperiode)
                }
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

                håndterYtelser(1.vedtaksperiode)
                håndterSimulering(1.vedtaksperiode)
                håndterUtbetalingsgodkjenning(1.vedtaksperiode)
                håndterUtbetalt()

                håndterYtelser(2.vedtaksperiode)
                håndterVilkårsgrunnlag(2.vedtaksperiode)
                håndterYtelser(2.vedtaksperiode)

                val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

                assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

                assertEquals(17.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
                assertEquals(14.februar til 10.mars, inspektør.periode(2.vedtaksperiode))

                val januarutbetaling = inspektør.utbetaling(0).inspektør
                val februarutbetaling = inspektør.utbetaling(1).inspektør
                val revurderingJanuar = inspektør.utbetaling(2).inspektør
                val revurderingFebruar = inspektør.utbetaling(3).inspektør

                assertEquals(januarutbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
                assertEquals(1.januar til 31.januar, januarutbetaling.periode)
                assertEquals(16.januar til 10.mars, februarutbetaling.periode)
                assertEquals(1.januar til 10.mars, februarutbetaling.utbetalingstidslinje.periode())

                februarutbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                    assertEquals(2, oppdrag.size)
                    oppdrag[0].inspektør.also { linje ->
                        assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                        assertEquals(Endringskode.UEND, linje.endringskode)
                    }
                    oppdrag[1].inspektør.also { linje ->
                        assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                        assertEquals(Endringskode.NY, linje.endringskode)
                    }
                }
                assertEquals(0, februarutbetaling.personOppdrag.size)

                assertEquals(januarutbetaling.korrelasjonsId, revurderingJanuar.korrelasjonsId)
                assertEquals(1.januar til 31.januar, revurderingJanuar.periode)
                revurderingJanuar.arbeidsgiverOppdrag.also { oppdrag ->
                    assertEquals(1, oppdrag.size)
                    oppdrag[0].inspektør.also { linje ->
                        assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                        assertEquals(Endringskode.NY, linje.endringskode)
                    }
                }
                assertEquals(0, revurderingJanuar.personOppdrag.size)

                assertNotEquals(revurderingJanuar.korrelasjonsId, revurderingFebruar.korrelasjonsId)
                assertEquals(14.februar til 10.mars, revurderingFebruar.periode)
                assertEquals(0, revurderingFebruar.personOppdrag.size)
                assertEquals(1, revurderingFebruar.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.NY, revurderingFebruar.arbeidsgiverOppdrag.inspektør.endringskode)
                revurderingFebruar.arbeidsgiverOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
        )
    }

    private fun assertUtbetalingsdag(dato: LocalDate, dagtype: KClass<out Utbetalingsdag>, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt, orgnummer: String = a1) {
        val sisteUtbetaling = inspektør(orgnummer).utbetalinger.last().inspektør
        val utbetalingstidslinje = sisteUtbetaling.utbetalingstidslinje
        val dagen = utbetalingstidslinje[dato]

        assertEquals(dagtype, dagen::class)
        assertEquals(arbeidsgiverbeløp, dagen.økonomi.inspektør.arbeidsgiverbeløp)
        assertEquals(personbeløp, dagen.økonomi.inspektør.personbeløp)
    }

    private fun assertSykdomstidslinjedag(dato: LocalDate, dagtype: KClass<out Dag>, kommerFra: KClass<out SykdomstidslinjeHendelse>) {
        val dagen = inspektør.sykdomstidslinje[dato]
        assertEquals(dagtype, dagen::class)
        assertTrue(dagen.kommerFra(kommerFra))
    }

    @Test
    fun `overstyr tidslinje endrer to perioder samtidig`() {
        nyPeriode(1.januar til 9.januar, a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(10.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        assertEquals(4, inspektør.sykdomshistorikk.inspektør.elementer())
        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100),
            ManuellOverskrivingDag(10.januar, Dagtype.Feriedag)
        ), orgnummer = a1)
        assertEquals(5, inspektør.sykdomshistorikk.inspektør.elementer())
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(10.januar, Dag.Feriedag::class, OverstyrTidslinje::class)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `overstyrer sykedag på slutten av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertEquals(Utbetaling.Sendt, inspektør.utbetalingtilstand(1))
        assertNotEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
        assertEquals("SSSSHH SSSSSHH SSSSSHH SSUFS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `vedtaksperiode rebehandler informasjon etter overstyring fra saksbehandler`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellArbeidsgiverdag(18.januar)))
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            0,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertNotEquals(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
        assertEquals(19.januar, inspektør.utbetalinger.last().utbetalingstidslinje().sykepengeperiode()?.start)
    }

    @Test
    fun `grad over grensen overstyres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(30, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].grad)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt etter overstyring av grad`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(23.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `overstyrt til fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(listOf(Periode(2.januar, 17.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[0].tom)
        assertEquals(24.januar, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `Overstyring oppdaterer sykdomstidlinjene`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 3.januar)
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(manuellFeriedag(26.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("SSSHH SSSSSHH SSSSSHH SSSSF", inspektør.sykdomstidslinje.toShortString())
        assertEquals("PPPPP PPPPPPP PPPPNHH NNNNF", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av sykHelgDag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(20.januar, 21.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyringSykedag(20.januar til 21.januar)
        håndterYtelser(1.vedtaksperiode)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `Overstyring av utkast til revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }


    @Test
    fun `skal kunne overstyre dagtype i utkast til revurdering ved revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrInntekt(inntekt = 20000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        inspektør.utbetalinger(2.vedtaksperiode).also { utbetalinger ->
            assertEquals(2, utbetalinger.size)
            assertEquals(Utbetaling.Forkastet, utbetalinger.last().inspektør.tilstand)
        }
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
        assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
        assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

        assertTilstander(1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }
}
