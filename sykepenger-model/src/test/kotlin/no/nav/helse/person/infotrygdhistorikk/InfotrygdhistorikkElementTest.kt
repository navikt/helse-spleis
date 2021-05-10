package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt

internal class InfotrygdhistorikkElementTest {

    private companion object {
        private const val ORGNUMMER = "987654321"
        private val kilde = TestEvent.testkilde
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        resetSeed(1.januar)
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `lik historikk`() {
        val perioder = listOf(
            Friperiode(1.januar,  31.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        )
        val inntekter = listOf(
            Inntektsopplysning("orgnr", 1.januar, 25000.månedlig, true)
        )
        val arbeidskategorikoder = mapOf(
            "01" to 1.januar
        )
        val ugyldigePerioder = listOf(1.januar to 1.januar)
        assertEquals(historikkelement().hashCode(), historikkelement().hashCode())
        assertEquals(historikkelement(), historikkelement())
        assertNotEquals(historikkelement().hashCode(), historikkelement(perioder).hashCode())
        assertEquals(historikkelement(perioder).hashCode(), historikkelement(perioder).hashCode())
        assertEquals(historikkelement(perioder), historikkelement(perioder))
        assertEquals(historikkelement(inntekter = inntekter).hashCode(), historikkelement(inntekter = inntekter).hashCode())
        assertNotEquals(historikkelement(perioder, inntekter).hashCode(), historikkelement(inntekter = inntekter).hashCode())
        assertNotEquals(historikkelement(perioder, inntekter), historikkelement(inntekter = inntekter))
        assertEquals(
            historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode(),
            historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode()
        )
        assertNotEquals(historikkelement(perioder, inntekter).hashCode(), historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode())
        assertNotEquals(historikkelement(perioder, inntekter), historikkelement(perioder, inntekter, arbeidskategorikoder))
        assertNotEquals(historikkelement().hashCode(), historikkelement(ugyldigePerioder = ugyldigePerioder).hashCode())
        assertEquals(historikkelement(ugyldigePerioder = ugyldigePerioder).hashCode(), historikkelement(ugyldigePerioder = ugyldigePerioder).hashCode())
        assertEquals(historikkelement(ugyldigePerioder = ugyldigePerioder), historikkelement(ugyldigePerioder = ugyldigePerioder))
        assertNotEquals(historikkelement().hashCode(), historikkelement(harStatslønn = true).hashCode())
        assertEquals(historikkelement(harStatslønn = true).hashCode(), historikkelement(harStatslønn = true).hashCode())
        assertEquals(historikkelement(harStatslønn = true), historikkelement(harStatslønn = true))
    }

    @Test
    fun `like perioder`() {
        val ferie = Friperiode(1.januar,  31.januar)
        val ukjent = UkjentInfotrygdperiode(1.januar,  31.januar)
        val utbetalingAG1 = ArbeidsgiverUtbetalingsperiode("ag1", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        val utbetalingAG2 = ArbeidsgiverUtbetalingsperiode("ag2", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        assertEquals(ferie, ferie)
        assertEquals(ukjent, ukjent)
        assertNotEquals(ferie, ukjent)
        assertNotEquals(ferie.hashCode(), ukjent.hashCode())
        assertNotEquals(ferie, utbetalingAG1)
        assertNotEquals(ferie.hashCode(), utbetalingAG1.hashCode())
        assertNotEquals(utbetalingAG1, utbetalingAG2)
        assertNotEquals(utbetalingAG1.hashCode(), utbetalingAG2.hashCode())
        assertEquals(utbetalingAG1, utbetalingAG1)
        assertEquals(utbetalingAG1.hashCode(), utbetalingAG1.hashCode())
    }

    @Test
    fun `lik periode - avrunding`() {
        val prosent = 30.prosent
        val inntekt1 = 505.daglig(prosent)
        val inntekt2 = inntekt1.reflection { _, månedlig, _, _ -> månedlig }.månedlig
        val periode1 = ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  1.januar, prosent, inntekt1)
        val periode2 = ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  1.januar, prosent, inntekt2)
        assertNotEquals(inntekt1, inntekt2)
        assertEquals(periode1, periode2)
    }

    @Test
    fun `utbetalingstidslinje - ferie`() {
        val ferie = Friperiode(1.januar,  10.januar)
        val inspektør = UtbetalingstidslinjeInspektør(ferie.utbetalingstidslinje())
        assertEquals(10, inspektør.fridagTeller)
    }

    @Test
    fun `utbetalingstidslinje - ukjent`() {
        val ferie = UkjentInfotrygdperiode(1.januar,  10.januar)
        val inspektør = UtbetalingstidslinjeInspektør(ferie.utbetalingstidslinje())
        assertEquals(0, inspektør.size)
    }

    @Test
    fun `utbetalingstidslinje - utbetaling`() {
        val utbetaling = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig)
        val inspektør = UtbetalingstidslinjeInspektør(utbetaling.utbetalingstidslinje())
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `samlet utbetalingstidslinje`() {
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  20.januar),
            UkjentInfotrygdperiode(21.januar,  31.januar)
        ))
        element.utbetalingstidslinje().also {
            assertEquals(1.januar til 20.januar, it.periode())
        }
    }

    @Test
    fun `fjerner historiske utbetalinger`() {
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  20.januar),
            ArbeidsgiverUtbetalingsperiode("ag1", 21.januar,  31.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        ))
        element.fjernHistorikk(tidslinjeOf(10.NAV, 10.FRI, 11.NAV, 28.NAV, 31.NAV), "ag1", 1.januar).also {
            assertEquals(11.januar til 31.mars, it.periode())
            assertTrue(it.subset(11.januar til 20.januar).all { it is Fridag })
            assertTrue(it.subset(21.januar til 28.februar).all { it is UkjentDag })
            assertTrue(it.subset(1.mars til 31.mars).all { it is NavDag })
        }
        element.fjernHistorikk(tidslinjeOf(10.NAV, 10.FRI, 11.NAV, 28.NAV, 31.NAV), "ag1", 1.februar).also {
            assertEquals(1.mars til 31.mars, it.periode())
        }
    }

    @Test
    fun `fjerner ikke historikk fra andre arbeidsgivere`() {
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag2", 1.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  20.januar)
        ))
        element.fjernHistorikk(tidslinjeOf(10.NAV, 10.FRI, 11.NAV), "ag1", 1.januar).also {
            assertEquals(1.januar til 31.januar, it.periode())
            assertTrue(it.subset(1.januar til 10.januar).all { it is NavDag })
            assertTrue(it.subset(11.januar til 20.januar).all { it is Fridag })
            assertTrue(it.subset(21.januar til 31.januar).all { it is NavDag })
        }
    }

    @Test
    fun `sykdomstidslinje - ferie`() {
        val periode = Friperiode(1.januar,  10.januar)
        val inspektør = SykdomstidslinjeInspektør(periode.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Feriedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `sykdomstidslinje - ukjent`() {
        val periode = UkjentInfotrygdperiode(1.januar,  10.januar)
        val inspektør = SykdomstidslinjeInspektør(periode.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.isEmpty())
    }

    @Test
    fun `sykdomstidslinje - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig)
        val inspektør = SykdomstidslinjeInspektør(periode.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `sammenhengende tidslinje`() {
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  12.januar),
            ArbeidsgiverUtbetalingsperiode("ag2", 13.januar,  15.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 16.januar,  20.januar, 100.prosent, 25000.månedlig)
        ))
        val tidslinje = element.sykdomstidslinje()
        val inspektør = SykdomstidslinjeInspektør(tidslinje)
        assertTrue(inspektør.dager.values.none { it is Dag.UkjentDag })
        assertEquals(1.januar, tidslinje.skjæringstidspunkt())
    }

    @Test
    fun `historikk for - ferie`() {
        val periode = Friperiode(1.januar,  10.januar)
        val inspektør = SykdomstidslinjeInspektør(periode.historikkFor("orgnr", Sykdomstidslinje(), kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Feriedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for - ukjent`() {
        val periode = UkjentInfotrygdperiode(1.januar,  10.januar)
        val inspektør = SykdomstidslinjeInspektør(periode.historikkFor("orgnr", Sykdomstidslinje(), kilde))
        assertTrue(inspektør.dager.isEmpty())
    }

    @Test
    fun `historikk for - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig)
        val inspektør = SykdomstidslinjeInspektør(periode.historikkFor("ag1", Sykdomstidslinje(), kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for annet orgnr - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig)
        val inspektør = SykdomstidslinjeInspektør(periode.historikkFor("noe helt annet", Sykdomstidslinje(), kilde))
        assertTrue(inspektør.dager.isEmpty())
    }

    @Test
    fun `historikk for overskriver ikke`() {
        val sykdomstidslinje = 10.A + 5.n_ + 5.S
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  15.januar)
        ))
        val inspektør = SykdomstidslinjeInspektør(element.historikkFor("ag1", sykdomstidslinje))
        assertEquals(8, inspektør.dager.filter { it.value is Dag.Arbeidsdag }.size)
        assertEquals(2, inspektør.dager.filter { it.value is Dag.FriskHelgedag }.size)
        assertEquals(5, inspektør.dager.filter { it.value is Dag.Feriedag }.size)
        assertEquals(4, inspektør.dager.filter { it.value is Dag.Sykedag }.size)
        assertEquals(1, inspektør.dager.filter { it.value is Dag.SykHelgedag }.size)
    }

    @Test
    fun `ingen ukjente arbeidsgivere`() {
        val element = historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag3", 1.januar,  9.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 10.januar,  20.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag2", 10.januar,  20.januar, 100.prosent, 25000.månedlig),
        ))
        assertTrue(element.ingenUkjenteArbeidsgivere(listOf("ag1", "ag2", "ag3"), 1.januar))
        assertTrue(element.ingenUkjenteArbeidsgivere(listOf("ag1", "ag2"), 10.januar))
        assertFalse(element.ingenUkjenteArbeidsgivere(listOf("ag1", "ag2"), 1.januar))
        assertFalse(element.ingenUkjenteArbeidsgivere(listOf("ag1"), 10.januar))
    }

    @Test
    fun `hensyntar ikke statslønn i overlapp-validering`() {
        val element = historikkelement(harStatslønn = true)
        assertTrue(element.validerOverlappende(aktivitetslogg, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `statslønn lager error`() {
        val element = historikkelement(harStatslønn = true)
        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FORLENGELSE, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.INFOTRYGDFORLENGELSE, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(element.valider(it, Periodetype.OVERGANG_FRA_IT, 1.januar til 31.januar, 1.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
    }

    @Test
    fun `skjæringstidspunkt lik null resulterer i passert validering av redusert utbetaling`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(2.januar, 31.januar), null))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("07" to 1.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 23.januar), 1.januar))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering feiler ikke når det ikke er redusert utbetaling i Infotrygd, men skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 23.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal ikke feile når bruker har redusert utbetaling i Infotrygd, men skjæringstidspunkt i Spleis`() {
        val arbeidskategorikoder = mapOf("07" to 1.januar)
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(perioder = utbetalinger, arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(7.januar, 23.januar), 7.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "07" to 6.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal ikke feile når bruker ikke har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "01" to 6.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal ikke feile når utbetalingshistorikken er tom`() {
        val element = historikkelement()
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `direkteutbetaling til bruker støttes ikke ennå`() {
        val utbetalinger = listOf(
            PersonUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, false)
            )
        )
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr er ok`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode("1234", 1.januar,  3.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `flere inntektsopplysninger på samme orgnr er ok`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.februar, 1234.månedlig, true),
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `flere inntektsopplysninger gir ikke feil dersom de er gamle`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true),
                Inntektsopplysning("987654321", 1.januar.minusYears(1), 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `lager ikke warning når dagsats endrer seg i en sammenhengende periode som følge av Grunnbeløpjustering`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april,  30.april, 100.prosent, 2161.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai,  31.mai, 100.prosent, 2236.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.juni, 30.juni), 1.april))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `lager ikke warning når dagsats endres pga gradering i en sammenhengende periode`() {
        val gradering = .5
        val dagsats = 2468
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(
                ORGNUMMER, 1.januar, 31.januar, (100 * gradering).roundToInt().prosent, (dagsats * gradering).roundToInt().daglig
            ),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar,  28.februar, 100.prosent, dagsats.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.april, 30.april), 1.april))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `lager ikke warning når dagsats ikke endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  31.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar,  28.februar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.april, 30.april), 1.april))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler selv om ugyldig dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            ugyldigePerioder = listOf(5.januar to 5.januar)
        )

        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.mars, 1.mars), 1.mars))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig),
            Friperiode(5.januar,  20.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.januar,  25.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(25.januar, inspektør.sisteDag)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig),
            UkjentInfotrygdperiode(5.januar,  5.januar)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = Inspektør().apply { tidslinje.accept(this) }
        assertEquals(1.januar, inspektør.førsteDag)
        assertEquals(10.januar, inspektør.sisteDag)
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val element = historikkelement()
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 1.januar), 1.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 1.januar), 1.januar))
        assertTrue(aktivitetslogg.hasErrorsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(28.januar, 28.januar), 28.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(29.januar, 29.januar), 29.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig),
            UkjentInfotrygdperiode(1.januar,  10.januar)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.august, 1.august), 1.august))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.august, 1.august), 1.august))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `validering av inntektsopplysninger feiler ikke for skjæringstidspunkt null`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar,  5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            utbetalinger, listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(10.januar, 31.januar), null))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering gir warning hvis vi har to inntekter for samme arbeidsgiver på samme dato`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), null))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), null))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på forskjellig dato`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 2.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), null))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er 12 måneder før perioden`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar(2018), 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.februar(2019), 28.februar(2019)), null))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er før skjæringstidspunkt`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(2.januar, 31.januar), 2.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `nytt element er ikke låst`() {
        val element = historikkelement()
        assertTrue(element.kanSlettes())
    }

    @Test
    fun `lagring av vilkårsgrunnlag låser elementet`() {
        val element = historikkelement()
        element.lagreVilkårsgrunnlag(1.januar, VilkårsgrunnlagHistorikk())
        assertFalse(element.kanSlettes())
    }

    @Test
    fun `element uten inntekter låses ikke`() {
        val element = historikkelement(inntekter = emptyList())
        element.addInntekter(Person("", ""), aktivitetslogg)
        assertTrue(element.kanSlettes())
    }

    @Test
    fun `lagrer inntekter låser elementet`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true)
            )
        )
        element.addInntekter(Person("", ""), aktivitetslogg)
        assertFalse(element.kanSlettes())
    }

    @Test
    fun `legger til siste inntekt først i inntektshistorikk`() {
        val inntektshistorikk = Inntektshistorikk()
        Inntektsopplysning.lagreInntekter(
            listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            ), inntektshistorikk, UUID.randomUUID()
        )
        assertEquals(1234.månedlig, inntektshistorikk.grunnlagForSykepengegrunnlag(1.januar))
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>> = emptyList(),
        hendelseId: UUID = UUID.randomUUID(),
        harStatslønn: Boolean = false,
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = hendelseId,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder,
            ugyldigePerioder = ugyldigePerioder,
            harStatslønn = harStatslønn
        )

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
            dag: NavDag,
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
            dag: Fridag,
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
            dag: UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            visitDag(dag)
        }
    }
}
