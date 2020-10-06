package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
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
            assertForkastetPeriodeTilstander(
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
                AVSLUTTET,
                TIL_INFOTRYGD
            )
        }
    }

    @Test
    fun `Utbetalingslinje har annullerte dager`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        inspektør.also {
            assertTrue(
                inspektør.utbetalinger[1].utbetalingstidslinje().all{it is Utbetalingstidslinje.Utbetalingsdag.AnnullertDag}
            )
        }
    }

    @Test
    fun `Oppdrag har nullbetaling`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        inspektør.also {
            var fagsystemId : String
            it.arbeidsgiverOppdrag[0].let{oppdragFørAnnullering ->
                assertEquals(1, oppdragFørAnnullering.size)
                assertEquals(fom.plusDays(16), oppdragFørAnnullering.first().fom)
                assertEquals(tom, oppdragFørAnnullering.first().tom)
                assertEquals(8586, oppdragFørAnnullering.totalbeløp())
                fagsystemId = oppdragFørAnnullering.fagsystemId()
            }
            it.arbeidsgiverOppdrag[1].let{oppdragEtterAnnullering ->
                assertEquals(1, oppdragEtterAnnullering.size)
                assertEquals(fom.plusDays(16), oppdragEtterAnnullering.first().fom)
                assertEquals(tom, oppdragEtterAnnullering.first().tom)
                assertEquals(fagsystemId, oppdragEtterAnnullering.fagsystemId())
                assertEquals(0, oppdragEtterAnnullering.totalbeløp())
                assertTrue(oppdragEtterAnnullering.dagSatser().all { it.second == 0 })
            }
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
