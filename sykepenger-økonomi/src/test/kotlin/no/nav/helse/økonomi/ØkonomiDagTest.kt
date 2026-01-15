package no.nav.helse.økonomi

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Vedtaksperiodeberegning
import no.nav.helse.utbetalingstidslinje.maksimumUtbetalingsberegning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ØkonomiDagTest {

    @Test
    fun `Beløp er ikke 6G-begrenset`() {
        val sykepengegrunnlag = 1500.daglig
        val a1 = tidslinjeOf(2.NAV(500))
        val b1 = tidslinjeOf(2.NAV(500))
        val c1 = tidslinjeOf(2.NAV(500))
        val (a, b, c) = listOf(a1, b1, c1).betal(sykepengegrunnlag)
        assertØkonomi(a, 500.0)
        assertØkonomi(b, 500.0)
        assertØkonomi(c, 500.0)
    }

    @Test
    fun `dekningsgrunnlag rundes opp`() {
        val sykepengegrunnlag = 1200.75.daglig
        val a1 = tidslinjeOf(2.NAV(1200.75, 50.0))
        val (a) = listOf(a1).betal(sykepengegrunnlag)
        assertØkonomi(a, 600.0)
    }

    @Test
    fun `dekningsgrunnlag rundes ned`() {
        val sykepengegrunnlag = 1200.49.daglig
        val a1 = tidslinjeOf(2.NAV(1200.49, 50.0))
        val (a) = listOf(a1).betal(sykepengegrunnlag)
        assertØkonomi(a, 600.0)
    }

    @Test
    fun `Dekningsgrunnlag uten desimaler`() {
        val sykepengegrunnlag = 1201.daglig
        val a1 = tidslinjeOf(2.NAV(1201, 50.0))
        val (a) = listOf(a1).betal(sykepengegrunnlag)
        assertØkonomi(a, 601.0)
    }

    @Test
    fun `liten inntekt`() {
        val inntekt = 5000.månedlig
        val a1 = tidslinjeOf(1.NAV(inntekt.daglig))
        val (a) = listOf(a1).betal(inntekt)
        assertØkonomi(a, 231.0, 0.0)
        val b1 = tidslinjeOf(1.NAV(inntekt.daglig, 50))
        val (b) = listOf(b1).betal(inntekt)
        assertØkonomi(b, 115.0, 0.0)
        val c1 = tidslinjeOf(1.NAV(inntekt.daglig, refusjonsbeløp = (inntekt / 2).daglig))
        val (c) = listOf(c1).betal(inntekt)
        assertØkonomi(c, 115.0, 115.0)
    }

    @Test
    fun `Beløp er 6G-begrenset`() {
        val sykepengegrunnlag = 561804.årlig
        val a1 = tidslinjeOf(2.NAV(1200))
        val b1 = tidslinjeOf(2.NAV(1200))
        val c1 = tidslinjeOf(2.NAV(1200))
        val (a, b, c) = listOf(a1, b1, c1).betal(sykepengegrunnlag)
        assertØkonomi(a, 720.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 720.0)
    }

    @Test
    fun `Beløp med arbeidsdag`() {
        val sykepengegrunnlag = 561804.årlig
        val a1 = tidslinjeOf(2.NAV(1200))
        val b1 = tidslinjeOf(2.NAV(1200))
        val c1 = tidslinjeOf(2.ARB(1200))
        val (a, b, c) = listOf(a1, b1, c1).betal(sykepengegrunnlag)
        assertØkonomi(a, 720.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp medNavDag som har blitt avvist`() {
        val sykepengegrunnlag = 561804.årlig
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200)).avvis(listOf(januar), Begrunnelse.MinimumInntekt)
        val (a1, b1, c1) = listOf(a, b, c).betal(sykepengegrunnlag)
        assertØkonomi(a, null, null)
        assertØkonomi(a1, 720.0, 0.0)
        assertØkonomi(b, null, null)
        assertØkonomi(b1, 720.0, 0.0)
        assertØkonomi(c, null, null)
        assertØkonomi(c1, 0.0, 0.0)
    }

    @Test
    fun `Beløp med avvistdager`() {
        val sykepengegrunnlag = 561804.årlig
        val a1 = tidslinjeOf(2.NAV(1200))
        val b1 = tidslinjeOf(2.NAV(1200))
        val c1 = tidslinjeOf(2.AVV(1200, 100))
        val (a, b, c) = listOf(a1, b1, c1).betal(sykepengegrunnlag)
        assertØkonomi(a, 720.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    private fun assertØkonomi(tidslinje: Utbetalingstidslinje, arbeidsgiverbeløp: Double?, personbeløp: Double? = 0.0) {
        tidslinje.forEach {
            Assertions.assertEquals(arbeidsgiverbeløp?.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            Assertions.assertEquals(personbeløp?.daglig, it.økonomi.inspektør.personbeløp)
        }
    }

    private fun List<Utbetalingstidslinje>.betal(sykepengegrunnlagBegrenset6G: Inntekt, andreYtelser: (dato: LocalDate) -> Prosentdel = { 0.prosent }): List<Utbetalingstidslinje> {
        val input = mapIndexed { index, it ->
            Arbeidsgiverberegning(
                inntektskilde = Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker("a${index + 1}"),
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }

        return input.maksimumUtbetalingsberegning(sykepengegrunnlagBegrenset6G, andreYtelser).map { it.vedtaksperioder.single().utbetalingstidslinje }
    }
}
