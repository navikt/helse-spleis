package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class RevurderingInntektV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere enslig periode`() {
        val forventetEndring = 200.daglig
        val overstyrtInntekt = INNTEKT + forventetEndring
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(19.januar, 1431.daglig, INGEN, INNTEKT)
        håndterOverstyrInntekt(overstyrtInntekt, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        assertDag<Sykedag, NavDag>(
            dato = 17.januar,
            arbeidsgiverbeløp = 1431.daglig,
            personbeløp = forventetEndring,
            aktuellDagsinntekt = overstyrtInntekt
        )
        assertDiff(2200)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `skal kunne overstyre inntekt i utkast til revurdering ved revurdering av tidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        assertDag<Sykedag, NavDag>(
            dato = 22.januar,
            arbeidsgiverbeløp = 1431.daglig,
            personbeløp = INGEN,
            aktuellDagsinntekt = INNTEKT
        )

        håndterOverstyrTidslinje((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        assertDag<Dag.Feriedag, Utbetalingsdag.Fridag>(
            dato = 22.januar,
            arbeidsgiverbeløp = INGEN,
            personbeløp = INGEN,
            aktuellDagsinntekt = INNTEKT
        )
        håndterSimulering(2.vedtaksperiode)

        val overstyrtInntekt = 20000.månedlig
        håndterOverstyrInntekt(inntekt = overstyrtInntekt, skjæringstidspunkt = 1.januar)
        håndterYtelser(2.vedtaksperiode)
        assertDag<Sykedag, NavDag>(
            dato = 17.januar,
            arbeidsgiverbeløp = overstyrtInntekt.rundTilDaglig(),
            personbeløp = INGEN,
            aktuellDagsinntekt = overstyrtInntekt
        )
        håndterSimulering(2.vedtaksperiode)
        assertDiff(-21286)

        // 23075 = round((20000 * 12) / 260) * 25 (25 nav-dager i januar + februar 2018)
        assertEquals(23075, inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.totalbeløp())
        assertEquals("SSSSSHH SSSSSHH SSSSSFF FFFFFFF FSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString().trim())
        assertEquals("PPPPPPP PPPPPPP PPNNNFF FFFFFFF FNNNNHH NNNNNHH NNNNNHH NNNNNHH NNN", inspektør.sisteUtbetalingUtbetalingstidslinje().toString().trim())

        assertTilstander(1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )

        assertTilstander(2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    private inline fun <reified D: Dag, reified UD: Utbetalingsdag>assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN, aktuellDagsinntekt: Inntekt = INGEN) {
        inspektør.sykdomshistorikk.inspektør.tidslinje(0)[dato].let {
            assertTrue(it is D) { "Forventet ${D::class.simpleName} men var ${it::class.simpleName}"}
        }
        inspektør.sisteUtbetalingUtbetalingstidslinje()[dato].let {
            assertTrue(it is UD) { "Forventet ${UD::class.simpleName} men var ${it::class.simpleName}"}
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
            assertEquals(aktuellDagsinntekt, it.økonomi.inspektør.aktuellDagsinntekt)
        }
    }

    private fun assertDiff(diff: Int) {
        assertEquals(diff, inspektør.utbetalinger.last().inspektør.nettobeløp)
    }
}