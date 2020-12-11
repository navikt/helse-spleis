package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtbetalingshistorikkInfotrygdperiodeTest {
    private companion object {
        private val EN_PERIODE = Periode(1.mars, 1.mars)
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `ugyldig periode`() {
        assertDoesNotThrow {
            Utbetalingshistorikk.Infotrygdperiode.Ugyldig(2.januar, 1.januar).valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        }
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(1.januar, 1.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        periode.valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(1.januar, inspektør.sisteDag)
    }

    @Test
    fun `ReduksjonArbeidsgiverRefusjon mappes til utbetalingstidslinje`() {
        val periode =
            Utbetalingshistorikk.Infotrygdperiode.ReduksjonArbeidsgiverRefusjon(1.januar, 1.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        periode.valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(1.januar, inspektør.sisteDag)
    }

    @Test
    fun `Utbetaling mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Utbetaling(1.januar, 1.januar, 1234.daglig, 100.prosent, "81549300")
        periode.valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(1.januar, inspektør.sisteDag)
    }

    @Test
    fun `ReduksjonMedlem mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.ReduksjonMedlem(1.januar, 1.januar, 1234.daglig, 100.prosent, "81549300")
        periode.valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(1.januar, inspektør.sisteDag)
    }

    @Test
    fun `Ferie mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Ferie(1.januar, 1.januar)
        periode.valider(aktivitetslogg, EN_PERIODE, ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(1.januar, inspektør.sisteDag)
    }

    @Test
    fun `Etterbetaling mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Etterbetaling(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `KontertRegnskap mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.KontertRegnskap(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Tilbakeført mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Tilbakeført(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Konvertert mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Konvertert(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Opphold mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Opphold(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Sanksjon mappes til utbetalingstidslinje`() {
        val periode = Utbetalingshistorikk.Infotrygdperiode.Sanksjon(1.januar, 1.januar)
        periode.valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        val tidslinje = periode.tidslinje()

        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Ukjent lager warning`() {
        Utbetalingshistorikk.Infotrygdperiode.Ukjent(1.januar, 1.januar).valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Ukjent mappes til tom utbetalingstidslinje`() {
        val tidslinje = Utbetalingshistorikk.Infotrygdperiode.Ukjent(1.januar, 1.januar)
            .tidslinje()
        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertNull(inspektør.førsteDag)
        assertNull(inspektør.sisteDag)
    }

    @Test
    fun `Ugyldig lager error`() {
        Utbetalingshistorikk.Infotrygdperiode.Ugyldig(1.januar, null).valider(aktivitetslogg, Periode(1.januar, 1.januar), ORGNUMMER)
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    private class Inspektør : UtbetalingsdagVisitor {
        var førsteDag: LocalDate? = null
        var sisteDag: LocalDate? = null

        private fun visitDag(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteDag = førsteDag ?: dag.dato
            sisteDag = dag.dato
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }
    }
}
