package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAVv2
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedArbeidsgiverBeløp
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingFlereArbeidsgivereTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `betaler flere arbeidsgivere over mai -- nytt grunnbeløp skal ikke gjelde`() {
        val ag1 = { tidslinjeOf(14.NAVv2(dekningsgrunnlag = 3000), startDato = 25.april) to 21610.0 }
        val ag2 = { tidslinjeOf() to 0.0 }
        assert6GBegrensetUtbetaling(ag1(), ag2())
        assert6GBegrensetUtbetaling(ag2(), ag1())
    }

    private fun assert6GBegrensetUtbetaling(ag1: Pair<Utbetalingstidslinje, Double>, ag2: Pair<Utbetalingstidslinje, Double>) {
        val dato = Utbetalingstidslinje.periode(listOf(ag1.first, ag2.first)).start
        val maksDagsats = Grunnbeløp.`6G`.dagsats(dato)
        MaksimumUtbetaling(listOf(ag2.first, ag1.first), aktivitetslogg, dato).betal()
        assertTrue(ag1.first.inspektør.økonomi.all { reflectedArbeidsgiverBeløp(it).daglig == maksDagsats }) {
            "noen dager har fått nytt grunnbeløp"
        }
        assertTrue(ag2.first.inspektør.økonomi.all { reflectedArbeidsgiverBeløp(it).daglig == maksDagsats }) {
            "noen dager har fått nytt grunnbeløp"
        }
        assertEquals(ag1.second, ag1.first.inspektør.totalUtbetaling())
        assertEquals(ag2.second, ag2.first.inspektør.totalUtbetaling())
    }
}
