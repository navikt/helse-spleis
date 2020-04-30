package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.august
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingshistorikkTest {
    private val graderingsliste = listOf(Utbetalingshistorikk.Graderingsperiode(1.januar, 31.januar, 100.0))
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `arbeidsgiverperioden regnes som gjennomført når siste utbetalingsdag er tilstøtende`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        assertTrue(utbetalingshistorikk.arbeidsgiverperiodeGjennomført(6.januar))
        assertTrue(utbetalingshistorikk.arbeidsgiverperiodeGjennomført(7.januar))
        assertTrue(utbetalingshistorikk.arbeidsgiverperiodeGjennomført(8.januar))
        assertFalse(utbetalingshistorikk.arbeidsgiverperiodeGjennomført(9.januar))
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        val tidslinje = utbetalingshistorikk.utbetalingstidslinje(1.januar)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `Gammel ugyldig periode ignoreres ved bygging av utbetalingstidslinje`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(17.januar, 31.januar, 1234),
            Utbetalingshistorikk.Periode.Ugyldig(1.januar(2017), 10.januar(2017))
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        val tidslinje = utbetalingshistorikk.utbetalingstidslinje(1.januar)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(17.januar, inspektør.førsteDag)
        assertEquals(31.januar, inspektør.sisteDag)
        assertEquals(11, inspektør.navDagTeller)
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.Ferie(5.januar, 20.januar, 0),
            Utbetalingshistorikk.Periode.Utbetaling(15.januar, 25.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        val tidslinje = utbetalingshistorikk.utbetalingstidslinje(1.januar)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(25.januar, inspektør.sisteDag)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Slår sammen reduksjonTilArbeidsgiver og reduksjonMedlem og ignorerer tilbakeført og sanksjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.Tilbakeført(5.januar, 20.januar),
            Utbetalingshistorikk.Periode.Sanksjon(15.januar, 25.januar),
            Utbetalingshistorikk.Periode.ReduksjonMedlem(20.januar, 31.januar, 623)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        val tidslinje = utbetalingshistorikk.utbetalingstidslinje(1.januar)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(31.januar, inspektør.sisteDag)
        assertEquals(16, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.Ukjent(5.januar, 5.januar)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        val tidslinje = utbetalingshistorikk.utbetalingstidslinje(1.januar)
        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
    }

    @Test
    fun `Feiler ikke selv om ugyldig dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.Ugyldig(5.januar, 5.januar)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        assertDoesNotThrow { utbetalingshistorikk.utbetalingstidslinje(1.januar) }
        assertTrue(utbetalingshistorikk.valider(Periode(1.mars, 1.mars)).hasErrors())
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = emptyList(),
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar))
        assertFalse(aktivitetslogg.hasWarnings()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar))
        assertTrue(aktivitetslogg.hasErrors()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(28.januar, 28.januar))
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasOnlyInfoAndNeeds())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(29.januar, 29.januar))
        assertFalse(aktivitetslogg.hasErrors()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234),
            Utbetalingshistorikk.Periode.Ukjent(1.januar,10.januar)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(1.august, 1.august))
        assertFalse(aktivitetslogg.hasWarnings()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234)
        )
        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList(),
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        utbetalingshistorikk.valider(Periode(1.august, 1.august))
        assertFalse(aktivitetslogg.hasWarnings()) { aktivitetslogg.toString() }
    }

    private class Inspektør : UtbetalingsdagVisitor {
        var førsteDag: LocalDate? = null
        var sisteDag: LocalDate? = null
        var navDagTeller: Int = 0

        private fun visitDag(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteDag = førsteDag ?: dag.dato
            sisteDag = dag.dato
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            visitDag(dag)
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            navDagTeller += 1
            visitDag(dag)
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            visitDag(dag)
        }

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            visitDag(dag)
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            visitDag(dag)
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            visitDag(dag)
        }

        override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            visitDag(dag)
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            visitDag(dag)
        }
    }
}
