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

internal class AnnulleringTest : AbstractEndToEndTest() {

    private var vedtaksperiodeTeller: Int = 0

    @Test
    fun `Annullering oppdaterer sykdomstidslinje og utbetalingstidslinje`() {
        nyttVedtak(3.januar, 26.januar, 100, 3.januar)
        håndterAnnullering()
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()

        assertEquals(
            Sykdomstidslinje.annullerteDager(3.januar til 26.januar, TestEvent.testkilde),
            inspektør.sykdomstidslinje
        )
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

            assertTrue(
                inspektør.utbetalinger[1].utbetalingstidslinje().all{it is Utbetalingstidslinje.Utbetalingsdag.AnnullertDag}
            )

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
