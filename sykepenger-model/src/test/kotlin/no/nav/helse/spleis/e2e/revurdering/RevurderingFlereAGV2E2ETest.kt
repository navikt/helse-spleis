package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Dagtype.Sykedag
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RevurderingFlereAGV2E2ETest : AbstractDslTest() {

    @Test
    fun `revurdere første periode - flere ag - ag 1`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 1`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag1`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)))
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdere første periode - flere ag - ag 2`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag))) }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 2`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag))) }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag2`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        nullstillTilstandsendringer()
        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag))) }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `starte revurdering av ag1 igjen etter at ag2 har startet revurdering`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 100)))
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juni, 10.juni, 100.prosent))
        }

        a1 {
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `revurdering av ag 2 mens ag 1 er til utbetaling`() {
        listOf(a1, a2).nyeVedtak(januar)
        nyPeriode(februar, a1, a2)
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }
        nullstillTilstandsendringer()

        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        a1 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        nullstillTilstandsendringer()
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }

        nullstillTilstandsendringer()

        a2 {
            håndterYtelser(2.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `revurdering av tidligere frittstående periode hos ag3 mens overlappende hos ag1 og ag2 utbetales`() {
        a3 { nyttVedtak(januar) }

        listOf(a1, a2).nyeVedtak(mai)
        nyPeriode(juni, a1, a2)
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }
        nullstillTilstandsendringer()

        a3 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }
        a1 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING)
            assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode])
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a3 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }

        nullstillTilstandsendringer()
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a3 {
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `revurdering av ag 1 kicker i gang revurdering av ag 2 - holder igjen senere perioder hos ag1`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)

        a1 { nyttVedtak(april) }

        nullstillTilstandsendringer()
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt for ag 1 mens senere periode for ag 1 er til utbetaling`() {
        /* Rekkefølge ting burde skje i:
        * 1. a1 v2 utbetales, a1 v1 avventer revurdering, a2 v1 avventer andre arbeidsgivere
        * 2. a1 v2 utbetalt, a1 v1 revurderes, a2 v1 avventer andre arbeidsgivere
        * 3. a1 v1 revurdert, a1 v2 revurderes, a2 v1 avventer andre arbeidsgivere
        * */
        a1 { nyttVedtak(januar) }
        a1 {
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 2.vedtaksperiode)
        }
        a2 {
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars))
        }
        a1 {
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        nullstillTilstandsendringer()

        a1 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING)
            assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode])
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `tre ag der a1 og a3 har to førstegangsbehandlinger - første førstegang på a1 blir revurdert mens andre førstegang på a1 er til utbetaling`() {
        listOf(a1, a3).nyeVedtak(januar)
        a1 {
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 2.vedtaksperiode)
        }
        a2 {
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars))
        }
        a3 {
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 2.vedtaksperiode)
        }
        a1 {
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        nullstillTilstandsendringer()

        a1 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a3 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING)
            assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode])
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a3 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `revurdering av ag 2 mens ag 1 revurderes og er til utbetaling`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()
        a1 { assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter()) }

        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }

        a1 {
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        nullstillTilstandsendringer()
        a1 { håndterUtbetalt() }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        nullstillTilstandsendringer()
        a1 { håndterYtelser(1.vedtaksperiode) }
        a2 { assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter()) }
        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `validerer ytelser på alle arbeidsgivere ved revurdering av flere arbeidgivere`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(20.januar til 31.januar, 100)))
            assertVarsler(listOf(RV_AY_5, RV_UT_23), 1.vedtaksperiode.filter())
        }
        a2 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
    }

    @Test
    fun `validerer ytelser for periode som strekker seg fra skjæringstidsunkt til siste tom`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        a2 { forlengVedtak(mars) }

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
            håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(20.mars til 31.mars, 100)))
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
        a2 {
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
            assertVarsler(emptyList(), 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `forkaster gamle utbetalinger for flere AG når der skjer endringer siden forrige`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)

        a1 {
            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(17.januar, Feriedag),
                    ManuellOverskrivingDag(18.januar, Feriedag)
                )
            )
            håndterYtelser(1.vedtaksperiode)

            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(18.januar, Sykedag, 100)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(3, utbetalinger.size)
            }
            inspektør.utbetalinger(2.vedtaksperiode).also { utbetalinger ->
                assertEquals(1, utbetalinger.size)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(3, utbetalinger.size)
                assertEquals(100, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar].økonomi.inspektør.totalGrad)
            }
            inspektør.utbetalinger(2.vedtaksperiode).also { utbetalinger ->
                assertEquals(1, utbetalinger.size)
            }
        }
    }

    @Test
    fun `Varsel på perioder hos begge AG dersom grad er under 20 prosent`() {
        listOf(a1, a2).nyeVedtak(januar)

        a1 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 19))) }
        a2 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Sykedag, 19))) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertEquals(19, inspektør.sykdomstidslinje.inspektør.grader[17.januar])
            assertVarsler(listOf(RV_VV_4, RV_UT_23), 1.vedtaksperiode.filter())
        }
        a2 {
            assertEquals(19, inspektør.sykdomstidslinje.inspektør.grader[17.januar])
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `revurdere på en eldre arbeidsgiver - infotrygd har utbetalt`() {
        a1 { nyttVedtak(januar) }
        nyPeriode(1.februar til 18.februar, a2)

        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 { håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag))) }

        nullstillTilstandsendringer()
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(a2, 17.januar, 18.februar)
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_UT_23, Varselkode.RV_IT_3), 1.vedtaksperiode.filter())

            val utbetaling1 = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)

            assertEquals(utbetaling1.korrelasjonsId, revurdering.korrelasjonsId)
            val oppdragInspektør = revurdering.arbeidsgiverOppdrag.inspektør
            assertEquals(Endringskode.ENDR, oppdragInspektør.endringskode)
            assertEquals(2, oppdragInspektør.antallLinjer())
            revurdering.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(Endringskode.ENDR, linje.endringskode)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(17.januar, linje.datoStatusFom)
                assertEquals("OPPH", linje.statuskode)
            }
            revurdering.arbeidsgiverOppdrag[1].inspektør.also { linje ->
                assertEquals(Endringskode.NY, linje.endringskode)
                assertEquals(18.januar til 31.januar, linje.fom til linje.tom)
                assertNull(linje.datoStatusFom)
                assertNull(linje.statuskode)
            }

            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }
}
