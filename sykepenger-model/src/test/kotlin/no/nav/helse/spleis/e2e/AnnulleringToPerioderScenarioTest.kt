package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AnnulleringToPerioderScenarioTest : AbstractEndToEndTest() {

    val fom = 3.februar
    val tom = 26.februar
    private var vedtaksperiodeTeller: Int = 0


    private fun eventsFremTilUtbetaling() {
        nyttVedtak(1.januar, 31.januar, 100, 1.januar)
        nyttVedtak(fom, tom, 100, 1.januar)
        håndterAnnullering(2.vedtaksperiode, fom, tom)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
    }

    @Test
    fun `Sykdomstidslinje har annullerte dager`() {
        eventsFremTilUtbetaling()
        assertTrue(inspektør.sykdomstidslinje.subset(fom til tom)
            .all { it is Dag.AnnullertDag })
        assertFalse(inspektør.sykdomstidslinje.subset(1.januar til fom.minusDays(1))
            .any { it is Dag.AnnullertDag })
    }

    @Test
    fun `Ingen feil`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt(2.vedtaksperiode)

        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
        }
    }

    @Test
    fun `Statusløp viser to godkjenninger og sender til infotrygd`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt(2.vedtaksperiode)

        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
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
        }
        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
            assertTilstander(
                2.vedtaksperiode,
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
        }
    }

    @Test
    fun `Utbetalingslinje har annullerte dager`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt(2.vedtaksperiode)
        TestTidslinjeInspektør(inspektør.utbetalingstidslinjer(1)).also { tidslinjeInspektør ->
            assertEquals(1, tidslinjeInspektør.dagtelling.size)
            assertEquals(24, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AnnullertDag::class])
        }
    }

    @Test
    fun `Oppdrag har nullbetaling`() {
        nyttVedtak(1.januar, 31.januar, 100, 1.januar)
        nyttVedtak(fom, tom, 100, 1.januar)
        håndterAnnullering(2.vedtaksperiode, fom, tom)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        inspektør.also {
            val enPeriode = it.arbeidsgiverOppdrag[0]
            assertEquals(1, enPeriode.size)
            assertEquals(1.januar.plusDays(16), enPeriode.first().fom)
            assertEquals(31.januar, enPeriode.first().tom)
            assertEquals(15741, enPeriode.totalbeløp())
            val fagsystemId = enPeriode.fagsystemId()

            val førAnnullering = it.arbeidsgiverOppdrag[1]
            assertEquals(2, førAnnullering.size)
            assertEquals(fom.plusDays(2), førAnnullering.last().fom)
            assertEquals(tom, førAnnullering.last().tom)
            assertEquals(38637, førAnnullering.totalbeløp())
            assertEquals(fagsystemId, førAnnullering.fagsystemId())

            val etterAnnullering = it.arbeidsgiverOppdrag[2]
            assertEquals(2, etterAnnullering.size)
            assertEquals(fagsystemId, etterAnnullering.fagsystemId())
            assertEquals(15741, etterAnnullering.totalbeløp())

            val annulleringslinje = etterAnnullering.last()
            assertEquals(fom, annulleringslinje.fom)
            assertEquals(tom, annulleringslinje.tom)
            assertEquals(0, annulleringslinje.beløp)
        }
    }


    private fun nyttVedtak(fom: LocalDate, tom: LocalDate, grad: Int, førsteFraværsdag: LocalDate) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad))
        vedtaksperiodeTeller += 1
        val id = vedtaksperiodeTeller.vedtaksperiode
        håndterInntektsmeldingMedValidering(
            id,
            listOf(Periode(fom, fom.plusDays(15))),
            førsteFraværsdag = førsteFraværsdag
        )
        håndterSøknadMedValidering(id, Søknad.Søknadsperiode.Sykdom(fom, tom, grad))
        håndterVilkårsgrunnlag(id, INNTEKT)
        håndterYtelser(id)   // No history
        håndterSimulering(id)
        håndterUtbetalingsgodkjenning(id, true)
        håndterUtbetalt(id, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
    }

}
