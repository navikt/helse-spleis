package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingFilterFlereArbeidsgivereTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `betaler flere arbeidsgivere over mai -- nytt grunnbeløp skal ikke gjelde`() {
        val ag1 = { tidslinjeOf(14.NAV(dekningsgrunnlag = 3000), startDato = 25.april) to 21610.0 }
        val ag2 = { tidslinjeOf() to 0.0 }
        assert6GBegrensetUtbetaling(ag1(), ag2())
        assert6GBegrensetUtbetaling(ag2(), ag1())
    }

    private fun assert6GBegrensetUtbetaling(ag1: Pair<Utbetalingstidslinje, Double>, ag2: Pair<Utbetalingstidslinje, Double>) {
        val periode = Utbetalingstidslinje.periode(listOf(ag1.first, ag2.first))
        val dato = periode.start
        val maksDagsats = Grunnbeløp.`6G`.dagsats(dato)

        val resultat = MaksimumUtbetalingFilter().betal(listOf(ag2.first, ag1.first), periode, aktivitetslogg, MaskinellJurist())

        val arbeidsgiverbeløp = { økonomi: Økonomi -> visitArbeidsgiverbeløp(økonomi) }

        assertTrue(resultat.first().inspektør.økonomi(arbeidsgiverbeløp).all { it.daglig == maksDagsats }) {
            "noen dager har fått nytt grunnbeløp"
        }
        assertTrue(resultat.last().inspektør.økonomi(arbeidsgiverbeløp).all { it.daglig == maksDagsats }) {
            "noen dager har fått nytt grunnbeløp"
        }
        assertEquals(ag1.second, resultat.last().inspektør.totalUtbetaling())
        assertEquals(ag2.second, resultat.first().inspektør.totalUtbetaling())
    }

    private fun visitArbeidsgiverbeløp(økonomi: Økonomi): Int {
        var res: Int? = null
        økonomi.accept(object: ØkonomiVisitor {
            override fun visitAvrundetØkonomi(grad: Int, arbeidsgiverRefusjonsbeløp: Int, dekningsgrunnlag: Int, totalGrad: Int, aktuellDagsinntekt: Int, arbeidsgiverbeløp: Int?, personbeløp: Int?, er6GBegrenset: Boolean?) {
                res = arbeidsgiverbeløp
            }
        })
        return res!!
    }
}
