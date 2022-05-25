package no.nav.helse.økonomi

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.september
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ØkonomiDagTest {

    @Test
    fun `Beløp er ikke 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(500))
        val b = tidslinjeOf(2.NAV(500))
        val c = tidslinjeOf(2.NAV(500))
        listOf(a, b, c).betal()
        assertØkonomi(a, 500.0)
        assertØkonomi(b, 500.0)
        assertØkonomi(c, 500.0)
    }

    @Test
    fun `dekningsgrunnlag rundes opp`() {
        val a = tidslinjeOf(2.NAV(1200.75, 50.0))
        listOf(a).betal()
        assertØkonomi(a, 601.0)
    }

    @Test
    fun `dekningsgrunnlag rundes ned`() {
        val a = tidslinjeOf(2.NAV(1200.49, 50.0))
        listOf(a).betal()
        assertØkonomi(a, 600.0)
    }

    @Test
    fun `Dekningsgrunnlag uten desimaler`() {
        val a = tidslinjeOf(2.NAV(1201, 50.0))
        listOf(a).betal()
        assertØkonomi(a, 601.0)
    }

    @Test
    fun `liten inntekt`() {
        val inntekt = 5000.månedlig
        val a = tidslinjeOf(1.NAV(inntekt))
        listOf(a).betal()
        assertØkonomi(a, 231.0, 0.0)
        val b = tidslinjeOf(1.NAV(inntekt, 50))
        listOf(b).betal()
        assertØkonomi(b, 116.0, 0.0)
        val c = tidslinjeOf(1.NAV(inntekt, refusjonsbeløp = inntekt/2))
        listOf(c).betal()
        assertØkonomi(c, 115.0, 116.0)
    }

    @Test
    fun `Beløp er 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
        listOf(a, b, c).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 720.0)
    }

    @Test
    fun `bruker riktig G-verdi ved 6G-begrensning`() {
        tidslinjeOf(2.NAV(3000)).let { tidslinje ->
            listOf(tidslinje).betal(2.januar)
            assertØkonomi(tidslinje, 2161.0)
        }
        tidslinjeOf(2.NAV(3000), startDato = 30.april).let { tidslinje ->
            listOf(tidslinje).betal(2.mai)
            assertØkonomi(tidslinje, 2161.0)
        }
    }

    @Test
    fun `bruker virkningsdato for å finne aktuell G-verdi ved begrensning`() {
        val skjæringstidspunkt = 1.mai(2020)
        val virkningsdatoForNyttGrunnbeløp = 21.september(2020)
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            listOf(tidslinje).betal(skjæringstidspunkt)
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt).rundTilDaglig().reflection { _, _, daglig, _ -> daglig}) // 2019-grunnbeløp
        }
        tidslinjeOf(2.NAV(3000), startDato = skjæringstidspunkt).let { tidslinje ->
            listOf(tidslinje).betal(virkningsdatoForNyttGrunnbeløp)
            assertØkonomi(tidslinje, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningsdatoForNyttGrunnbeløp).rundTilDaglig().reflection { _, _, daglig, _ -> daglig}) // 2020-grunnbeløp fordi virkningsdato er passert
        }
    }

    @Test
    fun `Beløp med arbeidsdag`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.ARB(1200))
        listOf(a, b, c).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp medNavDag som har blitt avvist`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
            .onEach { it.avvis(listOf(Begrunnelse.MinimumInntekt)) }
        listOf(a, b, c).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp med avvistdager`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
        listOf(a, b, c).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 0.0)
    }

    @Test
    fun `Beløp med avvistdager som er låst opp`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.AVV(1200, 100))
            .onEach { (it as AvvistDag).navDag() }
        listOf(a, b, c).betal()
        assertØkonomi(a, 721.0)
        assertØkonomi(b, 720.0)
        assertØkonomi(c, 720.0)
    }

    private fun assertØkonomi(tidslinje: Utbetalingstidslinje, arbeidsgiverbeløp: Double, personbeløp: Double = 0.0) {
        tidslinje.forEach {
            it.økonomi.medData {
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    actualArbeidsgiverbeløp,
                    actualPersonbeløp,
                    _ ->
                assertEquals(arbeidsgiverbeløp, actualArbeidsgiverbeløp)
                assertEquals(personbeløp, actualPersonbeløp)
            }
        }
    }

    private fun List<Utbetalingstidslinje>.betal(virkningsdato: LocalDate = 1.januar) {
        val periode = virkningsdato til virkningsdato // Brukes ikke når vi eksplisitt setter virkningsdato
        MaksimumUtbetalingFilter { virkningsdato }.betal(this, periode, Aktivitetslogg(), MaskinellJurist())
    }
}
