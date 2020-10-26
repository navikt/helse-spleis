package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Test

internal class AnnulleringE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Annullering for en periode`()
    {
        val fom = 3.januar
        val tom = 26.januar

        nyttVedtak(fom, tom)
        håndterAnnullering()
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()

        sjekkAt(inspektør) {
            sykdomstidslinje er Sykdomstidslinje.annullerteDager(fom til tom, TestEvent.testkilde)
        }

        håndterUtbetalt()

        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()

            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_SØKNAD_FERDIG_GAP,
                AVVENTER_VILKÅRSPRØVING_GAP,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )

            TestTidslinjeInspektør(utbetalingstidslinjer(0)).apply {
                dagtelling[Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class] er 16
                dagtelling[Utbetalingstidslinje.Utbetalingsdag.AnnullertDag::class] er 8
            }

            val førAnnullering = arbeidsgiverOppdrag[0]
            førAnnullering.totalbeløp() er 8586

            val etterAnnullering = arbeidsgiverOppdrag[1]
            etterAnnullering.size er 1
            etterAnnullering.first().fom er fom.plusDays(16)
            etterAnnullering.first().tom er tom
            etterAnnullering.totalbeløp() er 0
            etterAnnullering.dagSatser().all { it.second == 0 } er sant
        }
    }

    @Test
    fun `Annullering av andre periode i en forlengelse med to perioder`() {
        val tom1 = 31.januar
        val fom1 = 1.januar
        val fom2 = 1.februar
        val tom2 = 28.februar

        nyttVedtak(fom1, tom1)
        forlengVedtak(fom2, tom2)
        håndterAnnullering(fom2, tom2)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        sjekkAt(inspektør) {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()

            sykdomstidslinje.subset(fom1 til tom1)
                .any { it is Dag.AnnullertDag } er usant
            sykdomstidslinje.subset(fom2 til tom2)
                .all { it is Dag.AnnullertDag } er sant

            TestTidslinjeInspektør(utbetalingstidslinjer(1)).also {
                it.dagtelling.size er 1
                it.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AnnullertDag::class] er 28
            }

            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_SØKNAD_FERDIG_GAP,
                AVVENTER_VILKÅRSPRØVING_GAP,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )

            arbeidsgiverOppdrag[0].totalbeløp() er 15741
            arbeidsgiverOppdrag[1].totalbeløp() er 44361

            val linjerEtterAnnullering = arbeidsgiverOppdrag[2]
            linjerEtterAnnullering.size er 2
            linjerEtterAnnullering.totalbeløp() er 15741

            val annulleringslinje = linjerEtterAnnullering.last()
            annulleringslinje.fom er fom2
            annulleringslinje.tom er tom2
            annulleringslinje.beløp er 0
        }
    }
}
