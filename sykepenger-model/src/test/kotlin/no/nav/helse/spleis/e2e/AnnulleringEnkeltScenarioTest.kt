package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AnnullertDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
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

        sjekkAt {
            sykdomstidslinje er Sykdomstidslinje.annullerteDager(fom til tom, TestEvent.testkilde)
        }
    }

    @Test
    fun `Ingen feil`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        sjekkAt {
            !personLogg.hasErrorsOrWorse() ellers personLogg.toString()
        }
    }

    @Test
    fun `Statusløp viser to godkjenninger og sender til infotrygd`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()

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

    @Test
    fun `Utbetalingslinje har annullerte dager`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        sjekkAt {
            TestTidslinjeInspektør(utbetalingstidslinjer(0)).apply {
                dagtelling[ArbeidsgiverperiodeDag::class] er 16
                dagtelling[AnnullertDag::class] er 8
            }
        }
    }

    @Test
    fun `Oppdrag har nullbetaling`() {
        eventsFremTilUtbetaling()
        håndterUtbetalt()
        sjekkAt {
            val førAnnullering = arbeidsgiverOppdrag[0]
            førAnnullering.size er 1
            førAnnullering.first().fom er fom.plusDays(16)
            førAnnullering.first().tom er tom
            førAnnullering.totalbeløp() er 8586
            val fagsystemId = førAnnullering.fagsystemId()

            val etterAnnullering = arbeidsgiverOppdrag[1]
            etterAnnullering.size er 1
            etterAnnullering.first().fom er fom.plusDays(16)
            etterAnnullering.first().tom er tom
            etterAnnullering.fagsystemId() er fagsystemId
            etterAnnullering.totalbeløp() er 0
            etterAnnullering.dagSatser().all { it.second == 0 } er sant
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
