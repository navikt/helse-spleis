package no.nav.helse.hendelser

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
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

    private fun utbetalingshistorikk(
        utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode>,
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning> = emptyList()
    ) =
        Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODEID,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            aktivitetslogg = aktivitetslogg
        )

    @Test
    fun `direkteutbetaling til bruker støttes ikke ennå`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234.månedlig, "123456789", false)
            )
        )

        assertTrue(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `flere inntektsopplysninger på ulike orgnr gir feil`() = Toggles.FlereArbeidsgivereOvergangITEnabled.disable {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.desember(2017), 1234.månedlig, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234.månedlig, ORGNUMMER, true)
            )
        )

        assertTrue(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr er ok`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 3.januar, 1234.daglig, 100.prosent, "1234"),
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

        assertFalse(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `forlengelser fra infotrygd med to tilstøtende perioder hvor den ene har forskjellig orgnr gir feil`() =
        Toggles.FlereArbeidsgivereOvergangITEnabled.disable {
            val utbetalinger = listOf(
                RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, "1234"),
                RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
            )
            val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

            assertTrue(
                utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
            )
        }

    @Test
    fun `forlengelser fra infotrygd med én tilstøtende periode med forskjellig orgnr gir feil`() = Toggles.FlereArbeidsgivereOvergangITEnabled.disable {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, "1234"),
            RefusjonTilArbeidsgiver(1.januar, 3.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(utbetalinger)

        assertTrue(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `flere inntektsopplysninger på samme orgnr er ok`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.februar, 1234.månedlig, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234.månedlig, "123456789", true)
            )
        )

        assertFalse(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `flere inntektsopplysninger gir ikke feil dersom de er gamle`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234.månedlig, "123456789", true),
                Utbetalingshistorikk.Inntektsopplysning(1.januar.minusYears(1), 1234.månedlig, "987654321", true)
            )
        )

        assertFalse(
            utbetalingshistorikk.valider(Periode(6.januar, 31.januar), 1.januar).hasErrorsOrWorse()
        )
    }

    @Test
    fun `lager ikke warning når dagsats endrer seg i en sammenhengende periode som følge av Grunnbeløpjustering`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.april, 30.april, 2161.daglig, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.mai, 31.mai, 2236.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.juni, 30.juni), 1.april).also {
            assertFalse(it.hasWarningsOrWorse())
        }
    }

    @Test
    fun `lager ikke warning når dagsats endres pga gradering i en sammenhengende periode`() {
        val gradering = .5
        val dagsats = 2468
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(
                1.januar, 31.januar, (dagsats * gradering).roundToInt().daglig,
                (100 * gradering).roundToInt().prosent,
                ORGNUMMER
            ),
            RefusjonTilArbeidsgiver(1.februar, 28.februar, dagsats.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april), 1.april).also {
            assertFalse(it.hasWarningsOrWorse())
        }
    }

    @Test
    fun `lager ikke warning når dagsats ikke endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 31.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.februar, 28.februar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )
        utbetalingshistorikk.valider(Periode(1.april, 30.april), 1.april).also {
            assertFalse(it.hasWarningsOrWorse())
        }
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Ferie(5.januar, 20.januar),
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(15.januar, 25.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(25.januar, inspektør.sisteDag)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Slår sammen reduksjonTilArbeidsgiver og reduksjonMedlem og ignorerer tilbakeført og sanksjon`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Tilbakeført(5.januar, 20.januar),
            Utbetalingshistorikk.Infotrygdperiode.Sanksjon(15.januar, 25.januar),
            Utbetalingshistorikk.Infotrygdperiode.ReduksjonMedlem(20.januar, 31.januar, 623.daglig, 100.prosent, ORGNUMMER)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(31.januar, inspektør.sisteDag)
        assertEquals(16, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Infotrygdperiode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Ukjent(5.januar, 5.januar)
        )

        val tidslinje = utbetalinger.map { it.tidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
    }

    @Test
    fun `Feiler selv om ugyldig dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            Utbetalingshistorikk.Infotrygdperiode.ReduksjonArbeidsgiverRefusjon(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Ugyldig(5.januar, 5.januar)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        assertTrue(utbetalingshistorikk.valider(Periode(1.mars, 1.mars), 1.mars).hasErrorsOrWorse())
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = emptyList(),
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar), 1.januar)
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.januar, 1.januar), 1.januar)
        assertTrue(aktivitetslogg.hasErrorsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(28.januar, 28.januar), 28.januar)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(29.januar, 29.januar), 29.januar)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Ukjent(1.januar, 10.januar)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.august, 1.august), 1.august)
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 10.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = emptyList()
        )

        utbetalingshistorikk.valider(Periode(1.august, 1.august), 1.august)
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `validering av inntektsopplysninger feiler ikke for skjæringstidspunkt null`() {
        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(1.januar, 5.januar, 1234.daglig, 100.prosent, ORGNUMMER)
        )
        val utbetalingshistorikk = utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(1.januar, 1234.månedlig, ORGNUMMER, true),
            )
        )

        assertFalse(
            utbetalingshistorikk.valider(Periode(10.januar, 31.januar), null).hasErrorsOrWorse()
        )
    }

    private class Inspektør : UtbetalingsdagVisitor {
        var førsteDag: LocalDate? = null
        var sisteDag: LocalDate? = null
        var navDagTeller: Int = 0

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
            navDagTeller += 1
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
