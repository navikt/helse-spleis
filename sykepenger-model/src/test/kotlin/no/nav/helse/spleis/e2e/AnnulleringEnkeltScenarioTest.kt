package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AnnullertDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AnnulleringEnkeltScenarioTest : AbstractEndToEndTest() {

    val fom = 3.januar
    val tom = 26.januar
    private var vedtaksperiodeTeller: Int = 0


    private fun eventsFremTilUtbetaling() {
        nyttVedtak(fom, tom, 100, fom)
        håndterAnnullering()
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
    }

    @Test
    fun `Sykdomstidslinje har annullerte dager`() {
        eventsFremTilUtbetaling()

        assertEquals(
            Sykdomstidslinje.annullerteDager(fom til tom, TestEvent.testkilde),
            inspektør.sykdomstidslinje
        )
    }

    @Test
    fun `Ingen feil`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()

        inspektør.also {
            assertFalse(it.personLogg.hasErrorsOrWorse(), it.personLogg.toString())
        }
    }

    @Test
    fun `Statusløp viser to godkjenninger og sender til infotrygd`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()

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
        håndterUtbetalt()
        TestTidslinjeInspektør(inspektør.utbetalingstidslinjer(0)).also { tidslinjeInspektør ->
            assertEquals(16, tidslinjeInspektør.dagtelling[ArbeidsgiverperiodeDag::class])
            assertEquals(8, tidslinjeInspektør.dagtelling[AnnullertDag::class])
        }
    }

    @Test
    fun `Oppdrag har nullbetaling`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        inspektør.also {
            val førAnnullering = it.arbeidsgiverOppdrag[0]
            assertEquals(1, førAnnullering.size)
            assertEquals(fom.plusDays(16), førAnnullering.first().fom)
            assertEquals(tom, førAnnullering.first().tom)
            assertEquals(8586, førAnnullering.totalbeløp())
            val fagsystemId = førAnnullering.fagsystemId()

            val etterAnnullering = it.arbeidsgiverOppdrag[1]
            assertEquals(1, etterAnnullering.size)
            assertEquals(fom.plusDays(16), etterAnnullering.first().fom)
            assertEquals(tom, etterAnnullering.first().tom)
            assertEquals(fagsystemId, etterAnnullering.fagsystemId())
            assertEquals(0, etterAnnullering.totalbeløp())
            assertTrue(etterAnnullering.dagSatser().all { it.second == 0 })
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
