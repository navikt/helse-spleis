package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

internal class InfotrygdhistorikkElementTest {

    internal companion object {
        private const val ORGNUMMER = "987654321"
        internal fun eksisterendeInfotrygdHistorikkelement(
            perioder: List<Infotrygdperiode> = emptyList(),
            inntekter: List<Inntektsopplysning> = emptyList(),
            arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
            hendelseId: UUID = UUID.randomUUID(),
            oppdatert: LocalDateTime = LocalDateTime.now(),
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) =
            InfotrygdhistorikkElement.ferdigElement(
                id = UUID.randomUUID(),
                tidsstempel = tidsstempel,
                hendelseId = hendelseId,
                infotrygdperioder = perioder,
                inntekter = inntekter,
                arbeidskategorikoder = arbeidskategorikoder,
                oppdatert = oppdatert,
            )
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
            Friperiode(1.januar, 31.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar, 28.februar, 100.prosent, 25000.månedlig)
        )
        val inntekter = listOf(
            Inntektsopplysning("orgnr", 1.januar, 25000.månedlig, true)
        )
        val arbeidskategorikoder = mapOf(
            "01" to 1.januar
        )
        assertEquals(nyttHistorikkelement(), nyttHistorikkelement())
        assertEquals(nyttHistorikkelement(perioder), nyttHistorikkelement(perioder))
        assertNotEquals(nyttHistorikkelement(perioder, inntekter), nyttHistorikkelement(inntekter = inntekter))
        assertNotEquals(nyttHistorikkelement(perioder, inntekter), nyttHistorikkelement(perioder, inntekter, arbeidskategorikoder))
        assertEquals(nyttHistorikkelement(), nyttHistorikkelement())
    }

    private fun assertEquals(one: InfotrygdhistorikkElement, other: InfotrygdhistorikkElement) {
        assertTrue(one.funksjoneltLik(other))
        assertTrue(other.funksjoneltLik(one))
    }

    @Test
    fun `person- og arbeidsgiverutbetaling på samme dag`() {
        val element1 = nyttHistorikkelement(
            perioder = listOf(
                ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig),
                PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig)
            )
        )
        val identiskElement = nyttHistorikkelement(
            perioder = listOf(
                PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig),
                ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig)
            )
        )
        assertEquals(element1, identiskElement)
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `sortering på inntekter har ikke betydning`() {
        val inntekt1 = Inntektsopplysning("orgnr", 1.januar, 1000.daglig, true)
        val inntekt2 = Inntektsopplysning("orgnr", 1.januar, 0.daglig, true)

        val element1 = nyttHistorikkelement(inntekter = listOf(inntekt1, inntekt2))
        val identiskElement = nyttHistorikkelement(inntekter = listOf(inntekt2, inntekt1))
        assertEquals(element1, identiskElement)
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `sortering på arbeidsgiverkategorikode har ikke betydning`() {
        val arbeidskategorikoder1 = mapOf(
            "01" to 1.januar,
            "02" to 2.januar
        )
        val arbeidskategorikoder2 = mapOf(
            "02" to 2.januar,
            "01" to 1.januar
        )
        val element1 = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder1)
        val identiskElement = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder2)
        assertEquals(arbeidskategorikoder1, arbeidskategorikoder2)
        assertEquals(element1, identiskElement)
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `samlet utbetalingstidslinje`() {
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 20.januar),
            )
        )
        element.utbetalingstidslinje().also {
            assertEquals(1.januar til 20.januar, it.periode())
        }
    }

    @Test
    fun `sammenhengende tidslinje`() {
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 12.januar),
                ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar, 100.prosent, 25000.månedlig),
                ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar, 100.prosent, 25000.månedlig)
            )
        )
        val tidslinje = element.sykdomstidslinje()
        assertTrue(tidslinje.inspektør.dager.values.none { it is Dag.UkjentDag })
    }

    @Test
    fun `historikk for overskriver`() {
        val sykdomstidslinje = 10.A + 5.opphold + 5.S
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 15.januar)
            )
        )
        val inspektør = element.historikkFor("ag1", sykdomstidslinje).inspektør
        assertEquals(0, inspektør.dager.filter { it.value is Dag.Arbeidsdag }.size)
        assertEquals(0, inspektør.dager.filter { it.value is Dag.FriskHelgedag }.size)
        assertEquals(5, inspektør.dager.filter { it.value is Dag.Feriedag }.size)
        assertEquals(12, inspektør.dager.filter { it.value is Dag.Sykedag }.size)
        assertEquals(3, inspektør.dager.filter { it.value is Dag.SykHelgedag }.size)
    }

    @Test
    fun `historikk for overskriver selv om periode er låst`() {
        val sykdomstidslinje = 28.S + 3.A + 16.S
        sykdomstidslinje.lås(1.januar til 31.januar)
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 29.januar, 31.januar, 100.prosent, 25000.månedlig)
            )
        )
        val inspektør = element.historikkFor("ag1", sykdomstidslinje).inspektør
        assertEquals(0, inspektør.dager.filter { it.value is Dag.Arbeidsdag }.size)
        assertEquals(0, inspektør.dager.filter { it.value is Dag.FriskHelgedag }.size)
        assertEquals(0, inspektør.dager.filter { it.value is Dag.Feriedag }.size)
        assertEquals(35, inspektør.dager.filter { it.value is Dag.Sykedag }.size)
        assertEquals(12, inspektør.dager.filter { it.value is Dag.SykHelgedag }.size)
    }

    @Test
    fun `historikk for utvider ikke`() {
        val sykdomstidslinje = 10.S
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 31.januar, 100.prosent, 25000.månedlig),
                Friperiode(1.februar, 28.februar)
            )
        )
        val tidslinje = element.historikkFor("ag1", sykdomstidslinje)
        assertEquals(1.januar til 28.februar, tidslinje.periode())
    }

    @Test
    fun `hver første utbetalingsdag har en tilhørende inntekt`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 3.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
            Inntektsopplysning(ORGNUMMER, 5.januar, 1234.månedlig, true)
        )

        val element = nyttHistorikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `første utbetalingsdag mangler en tilhørende inntekt`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 3.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true)
        )

        val element = nyttHistorikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `sjekker at vi har inntekt fra første dag i periodeb som har en utbetaling`() {
        val utbetalinger = listOf(
            Friperiode(1.januar, 4.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 5.januar, 1234.månedlig, true))

        val element = nyttHistorikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `perioder fra infortrygd slås sammen dersom gapet kun er helg`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 8.januar, 12.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true))
        val element = nyttHistorikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("07" to 1.januar)
        val element = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periode(6.januar, 23.januar), 1.januar, "ag1"))
        aktivitetslogg.assertFunksjonellFeil(Varselkode.RV_IT_15)
    }

    @Test
    fun `validering feiler ikke når det ikke er redusert utbetaling i Infotrygd, men skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar)
        val element = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periode(6.januar, 23.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `validering skal ikke feile når bruker har redusert utbetaling i Infotrygd, men skjæringstidspunkt i Spleis`() {
        val arbeidskategorikoder = mapOf("07" to 1.januar)
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true)
        )
        val element = nyttHistorikkelement(
            perioder = utbetalinger,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder
        )
        assertTrue(element.valider(aktivitetslogg, Periode(9.januar, 23.januar), 9.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "07" to 6.januar)
        val element = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        aktivitetslogg.assertFunksjonellFeil(Varselkode.RV_IT_15)
    }

    @Test
    fun `validering skal ikke feile når bruker ikke har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "01" to 6.januar)
        val element = nyttHistorikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `validering skal ikke feile når utbetalingshistorikken er tom`() {
        val element = nyttHistorikkelement()
        assertTrue(element.valider(aktivitetslogg, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr kastes ut`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))

        val element = nyttHistorikkelement(utbetalinger, inntekter)

        assertFalse(element.valider(aktivitetslogg, Periode(6.januar, 31.januar), 1.januar, ORGNUMMER))
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med annet orgnr kastes ut`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning("ag1", 1.januar, 1234.daglig, true))

        val element = nyttHistorikkelement(utbetalinger, inntekter)

        assertFalse(element.valider(aktivitetslogg, Periode(6.januar, 31.januar), 1.januar, "ag2"))
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `flere inntektsopplysninger på samme orgnr er ok`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val element = nyttHistorikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.februar, 1234.månedlig, true),
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periode(9.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `flere inntektsopplysninger gir ikke feil dersom de er gamle`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val element = nyttHistorikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true),
                Inntektsopplysning("987654321", 1.januar.minusYears(1), 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periode(9.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenFunksjonellFeil()
    }

    @Test
    fun `lager ikke warning når dagsats endrer seg i en sammenhengende periode som følge av Grunnbeløpjustering`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april, 30.april, 100.prosent, 2161.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai, 31.mai, 100.prosent, 2236.daglig)
        )
        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.april, 2161.daglig, true),
            Inntektsopplysning(ORGNUMMER, 1.mai, 2161.daglig, true)
        )

        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(2.juni, 30.juni), 1.april, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `lager ikke warning når dagsats endres pga gradering i en sammenhengende periode`() {
        val gradering = .5
        val dagsats = 2468
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(
                ORGNUMMER,
                1.januar,
                31.januar,
                (100 * gradering).roundToInt().prosent,
                (dagsats * gradering).roundToInt().daglig
            ),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, dagsats.daglig)
        )

        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, (dagsats * gradering).roundToInt().daglig, true),
            Inntektsopplysning(ORGNUMMER, 1.februar, dagsats.daglig, true)
        )

        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(1.april, 30.april), 1.april, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `lager ikke warning når dagsats ikke endrer seg i en sammenhengende periode`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true),
            Inntektsopplysning(ORGNUMMER, 1.februar, 1234.daglig, true)
        )
        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(1.april, 30.april), 1.april, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        aktivitetslogg.assertIngenVarsler()

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            Friperiode(5.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.januar, 25.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        aktivitetslogg.assertIngenVarsler()

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(25.januar, inspektør.sistedato)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val element = nyttHistorikkelement()
        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 1.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val element = nyttHistorikkelement(utbetalinger)
        assertFalse(element.valider(aktivitetslogg, Periode(1.januar, 1.januar), 1.januar, "ag1"))
        aktivitetslogg.assertFunksjonellFeil(Varselkode.RV_IT_3)
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))

        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(28.januar, 28.januar), 28.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(29.januar, 29.januar), 29.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )

        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(1.august, 1.august), 1.august, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = nyttHistorikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periode(1.august, 1.august), 1.august, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir melding hvis vi har to inntekter for samme arbeidsgiver på samme dato`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir ikke melding ved duplikate inntekter for samme arbeidsgiver på samme dato`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig refusjonsinformasjon`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, false),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig opphør i refusjon`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true, 15.januar),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på forskjellig dato`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 2.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er 12 måneder før perioden`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar(2018), 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar(2018), 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periode(1.februar(2019), 28.februar(2019)), 1.februar(2019), "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er før skjæringstidspunkt`() {
        val element = nyttHistorikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periode(2.januar, 31.januar), 2.januar, "ag1"))
        aktivitetslogg.assertIngenVarsler()
    }

    private fun nyttHistorikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = hendelseId,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder
        )


}
