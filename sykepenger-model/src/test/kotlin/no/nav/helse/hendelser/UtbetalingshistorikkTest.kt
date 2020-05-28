package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

class UtbetalingshistorikkTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private val VEDTAKSPERIODEID = UUID.randomUUID().toString()
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    private fun utbetalingshistorikk(utbetalinger: List<Utbetalingshistorikk.Periode>, inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning> = emptyList()) =
        Utbetalingshistorikk(AKTØRID, UNG_PERSON_FNR_2018, ORGNUMMER, VEDTAKSPERIODEID, utbetalinger, inntektshistorikk, aktivitetslogg)

    @Test
    fun `direkteutbetaling til bruker støttes ikke ennå`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234, "123456789", false)
            )
        )

        assertTrue(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `flere inntektsopplysninger på ulike orgnr gir feil`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.februar, 1234, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234, "987654321", true)
            )
        )

        assertTrue(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr er ok`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 3.januar, 1234, 100, "1234"),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

        assertFalse(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `forlengelser fra infotrygd med to tilstøtende perioder hvor den ene har forskjellig orgnr gir feil`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, "1234"),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

        assertTrue(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `forlengelser fra infotrygd med én tilstøtende periode med forskjellig orgnr gir feil`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, "1234"),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 3.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

        assertTrue(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `flere inntektsopplysninger på samme orgnr er ok`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.februar, 1234, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234, "123456789", true)
            )
        )

        assertFalse(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `flere inntektsopplysninger gir ikke feil dersom de er gamle`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar.minusYears(1), 1234, "987654321", true)
            )
        )

        assertFalse(utbetalingshistorikk.valider(Periode(6.januar, 31.januar)).hasErrors())
    }

    @Test
    fun `lager warning når dagsats endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.februar, 28.februar, 4321, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april)).also {
            assertTrue(it.hasWarnings())
            assertFalse(it.hasErrors())
            assertFalse(it.hasOnlyInfoAndNeeds())
        }
    }

    @Test
    fun `lager ikke warning når dagsats endrer seg i en sammenhengende periode som følge av Grunnbeløpjustering`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.april, 30.april, Grunnbeløp.`6G`.dagsats(1.april), 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.mai, 31.mai, Grunnbeløp.`6G`.dagsats(1.mai), 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.juni, 30.juni)).also {
            assertTrue(it.hasOnlyInfoAndNeeds())
        }
    }

    @Test
    fun `lager ikke warning når dagsats endres pga gradering i en sammenhengende periode`() {
        val gradering = .5
        val dagsats = 2468
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, (dagsats*gradering).roundToInt(), (100*gradering).roundToInt(), ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.februar, 28.februar, dagsats, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april)).also {
            assertTrue(it.hasOnlyInfoAndNeeds())
        }
    }

    @Test
    fun `lager ikke warning når dagsats og grad endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1234, 50, ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.februar, 28.februar, 2345, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april)).also {
            assertTrue(it.hasWarnings())
            assertFalse(it.hasErrors())
            assertFalse(it.hasOnlyInfoAndNeeds())
        }
    }

    @Test
    fun `lager ikke warning når dagsats ikke endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.februar, 28.februar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april)).also {
            assertTrue(it.hasOnlyInfoAndNeeds())
        }
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Ferie(5.januar, 20.januar),
            Utbetalingshistorikk.Periode.Utbetaling(15.januar, 25.januar, 1234, 100, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(25.januar, inspektør.sisteDag)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Slår sammen reduksjonTilArbeidsgiver og reduksjonMedlem og ignorerer tilbakeført og sanksjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Tilbakeført(5.januar, 20.januar),
            Utbetalingshistorikk.Periode.Sanksjon(15.januar, 25.januar),
            Utbetalingshistorikk.Periode.ReduksjonMedlem(20.januar, 31.januar, 623, 100, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarnings())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(31.januar, inspektør.sisteDag)
        assertEquals(16, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Ukjent(5.januar, 5.januar)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
    }

    @Test
    fun `Feiler selv om ugyldig dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Ugyldig(5.januar, 5.januar)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        assertTrue(utbetalingshistorikk.valider(Periode(1.mars, 1.mars)).hasErrors())
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = emptyList(),
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar))
        assertFalse(aktivitetslogg.hasWarnings()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar))
        assertTrue(aktivitetslogg.hasErrors()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(28.januar, 28.januar))
        assertFalse(aktivitetslogg.hasWarnings())
        assertTrue(aktivitetslogg.hasOnlyInfoAndNeeds())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(29.januar, 29.januar))
        assertFalse(aktivitetslogg.hasWarnings())
        assertTrue(aktivitetslogg.hasOnlyInfoAndNeeds())
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER),
            Utbetalingshistorikk.Periode.Ukjent(1.januar,10.januar)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.august, 1.august))
        assertFalse(aktivitetslogg.hasWarnings()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234, 100, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
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

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            navDagTeller += 1
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            visitDag(dag)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            visitDag(dag)
        }
    }
}
