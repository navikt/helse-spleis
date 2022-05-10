package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
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
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class RevurderingV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere første periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere periode med forlengelse i avventer godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjenning(1.februar, 28.februar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertForventetFeil(
            forklaring = "avventer uferdig støtter ikke gjenopptaBehandlingNy og ny tilstandsflyt forventer å bruke avventer tidligere eller overlappende perioder istedenfor avventer uferdig",
            nå = {
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `revurdere andre periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(5.februar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(5.februar, 0.0)
        assertDiff(-1431)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere tredje periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(5.mars, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(5.mars, 0.0)
        assertDiff(-1431)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere første to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(31.januar, 1431.0)
        assertDag<Sykedag, NavDag>(1.februar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.februar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(31.januar, 0.0)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(1.februar, 0.0)
        assertDiff(-2862)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere dager i arbeidsgiverperioden på tidligere utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, ArbeidsgiverperiodeDag>(5.januar,0.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, ArbeidsgiverperiodeDag>(5.januar, 0.0)
        assertDiff(0)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere periode med nyere førstegangsbehandling innenfor samme agp`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()
        assertDag<Sykedag, ArbeidsgiverperiodeDag>(5.januar,0.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, ArbeidsgiverperiodeDag>(5.januar, 0.0)
        assertDiff(0)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere nyere skjæringstidspunkt så revurdere eldste`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(15.februar, 1431.0)
        assertDag<Sykedag, NavDag>(19.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(15.februar, 0.0)
        assertForventetFeil(
            forklaring = """
                Nye revurderinger må diffe mot forrige utbetaling uavhengig av om det er en utbetaling eller revurdering.
                I tillegg må revurderinger alltid sørge for å kjøre frem hele utbetalingstidslinjen innenfor sin korrelasjonsid
            """,
            nå = { assertDiff(-2862) },
            ønsket = { assertDiff(-1431) }
        )

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere nyere arbeidsgiverperiode så revurdere eldste`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.mars, 1431.0)
        assertDag<Sykedag, NavDag>(19.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(3.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.mars, 0.0)
        assertDiff(-1431)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `revurdere eldre skjæringstidspunkt mens nyere revurdert skjæringstidspunkt utbetales`() {
        nyttVedtak(1.januar, 20.januar)
        forlengVedtak(21.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()

        assertDag<Sykedag, NavDag>(15.februar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        håndterYtelser(4.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(15.februar, 0.0)
        assertDiff(-1431)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)

        assertDag<SykHelgedag, NavHelgDag>(21.januar, 0.0)
        assertDag<Sykedag, NavDag>(19.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(21.januar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING)

        nullstillTilstandsendringer()
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(21.januar, 0.0)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.januar, 0.0)
        assertDiff(-1431)

        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdering av eldre skjæringstidspunkt mens nyere skjæringstidspunkt utbetales`() {
        nyttVedtak(1.januar, 31.januar)
        tilGodkjent(1.mars, 31.mars, 100.prosent, førsteFraværsdag = 1.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING)

        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Periode med to forlengelser - forlengelse nummer en blir revurdert, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `periode med forlengelse etterfulgt av kort periode - kort periode avsluttes ikke før revurdering er ferdig`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.april, 16.april, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 16.april, 100.prosent))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertForventetFeil(
            forklaring = "Vi vet ikke hva ønsket oppførsel egentlig bør være",
            nå = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
            }
        )
    }

    @Test
    fun `revurdere eldste skjæringstidspunkt så revurdere nyeste`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1. mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        assertTrue(inspektør.sykdomstidslinje[15.februar] is Dag.Feriedag)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til simulering, så revurdering på eldre`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(20.mars, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(20.mars, 0.0)
        assertDiff(-1431)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til godkjenning, så revurdering på eldre`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(20.mars, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(20.mars, 0.0)
        assertDiff(-1431)
        håndterSimulering(2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert og er til godkjenning, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(5.februar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(5.februar, 0.0)
        assertDiff(-1431)
        håndterSimulering(2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revudering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juni, 10.juni, 100.prosent))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertForventetFeil(
            nå = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
            }
        )
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengVedtak(1.juni, 30.juni)
            nullstillTilstandsendringer()
            nyttVedtak(1.januar, 31.januar)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengVedtak(1.juni, 30.juni)
            nullstillTilstandsendringer()
            nyPeriode(1.januar til 15.januar)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengTilGodkjenning(1.juni, 30.juni)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            nyPeriode(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

            assertForventetFeil(
                forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
                nå = {
                    assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
                },
                ønsket = {
                    assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
                }
            )
        }
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() = Toggle.RevurdereOutOfOrder.enable {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterUtbetalt()
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
            }
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }

    @Test
    fun `periode til utbetaling blir overstyrt`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }

    @Test
    fun `revurdert periode til utbetaling blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere førstegangsbehandling står i avventer simulering`() {
        nyttVedtak(1.januar, 31.januar)
        tilSimulering(1.mars, 31.mars, 100.prosent, 1.mars)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere førstegangsbehandling står i avventer godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        tilGodkjenning(1.mars, 31.mars, 100.prosent, 1.mars)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere forlengelse står i avventer simulering`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        forlengTilSimulering(1.april, 30.april, 100.prosent)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere forlengelse står i avventer godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        forlengTilGodkjenning(1.april, 30.april, 100.prosent)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere revurdering står i avventer simulering revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.mars, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.mars, 0.0)
        assertDiff(-1431)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdering på tidligere skjæringstidspunkt mens nyere revurdering står i avventer godkjenning revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.mars, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(19.mars, 0.0)
        assertDiff(-1431)
        håndterSimulering(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `fire skjæringstidspunkter der første og siste blir revurdert - kun skjæringstidspunkter med endring i utbetaling skal utbetales`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nyttVedtak(1.mai, 31.mai)
        nyttVedtak(1.juli, 31.juli)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(17.juli, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.juli, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode)
        assertDiff(0)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(3.vedtaksperiode)
        assertDiff(0)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(4.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.juli, 0.0)
        assertDiff(-1431)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET)
        assertTilstander(4.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdering der vi har vært innom Infotrygd`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.februar, INNTEKT, true),
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere nyeste periode når vi har vært innom Infotrygd, deretter eldste periode`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.februar, INNTEKT, true)
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mars, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere frem og tilbake mellom feriedag og sykedag hvor første overstyring blir utbetalt`() {
        nyttVedtak(1.januar, 31.januar)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(17.januar, 0.0)
        assertDiff(-1431)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Sykedag, 100)))
        håndterYtelser(1.vedtaksperiode)
        assertDag<Sykedag, NavDag>(17.januar, 1431.0)
        assertDiff(1431)
    }

    private inline fun <reified D: Dag, reified UD: Utbetalingsdag>assertDag(dato: LocalDate, beløp: Double) {
        inspektør.sykdomshistorikk.sykdomstidslinje()[dato].let {
            assertTrue(it is D) { "Forventet ${D::class.simpleName} men var ${it::class.simpleName}"}
        }
        inspektør.sisteUtbetalingUtbetalingstidslinje()[dato].let {
            assertTrue(it is UD) { "Forventet ${UD::class.simpleName} men var ${it::class.simpleName}"}
            assertEquals(beløp.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(INGEN, it.økonomi.inspektør.personbeløp)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.utbetalinger.last().inspektør.nettobeløp)
    }
}