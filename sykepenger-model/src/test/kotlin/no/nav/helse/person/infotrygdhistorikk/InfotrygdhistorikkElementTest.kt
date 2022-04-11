package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.hentInfo
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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

    private companion object {
        private const val ORGNUMMER = "987654321"
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
        val ugyldigePerioder = listOf(UgyldigPeriode(1.januar, 1.januar, 100))
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
    fun `person- og arbeidsgiverutbetaling på samme dag`() {
        val element1 = historikkelement(perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig),
            PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig)
        ))
        val identiskElement = historikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, 100.prosent, 1000.daglig)
        ))
        assertEquals(element1, identiskElement)
        assertEquals(element1.hashCode(), identiskElement.hashCode())
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `sortering på inntekter har ikke betydning`() {
        val inntekt1 = Inntektsopplysning("orgnr", 1.januar, 1000.daglig, true)
        val inntekt2 = Inntektsopplysning("orgnr", 1.januar, 0.daglig, true)

        val element1 = historikkelement(inntekter = listOf(inntekt1, inntekt2))
        val identiskElement = historikkelement(inntekter = listOf(inntekt2, inntekt1))
        assertEquals(element1, identiskElement)
        assertEquals(element1.hashCode(), identiskElement.hashCode())
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
        val element1 = historikkelement(arbeidskategorikoder = arbeidskategorikoder1)
        val identiskElement = historikkelement(arbeidskategorikoder = arbeidskategorikoder2)
        assertEquals(arbeidskategorikoder1, arbeidskategorikoder2)
        assertEquals(arbeidskategorikoder1.hashCode(), arbeidskategorikoder2.hashCode())
        assertEquals(element1, identiskElement)
        assertEquals(element1.hashCode(), identiskElement.hashCode())
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `lik ugyldig periode`() {
        val element1 = historikkelement(ugyldigePerioder = listOf(UgyldigPeriode(1.januar, 1.januar, 100)))
        val identiskElement = historikkelement(ugyldigePerioder = listOf(UgyldigPeriode(1.januar, 1.januar, 100)))
        assertEquals(element1, identiskElement)
        assertEquals(element1.hashCode(), identiskElement.hashCode())
    }

    @Test
    fun `samlet utbetalingstidslinje`() {
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 20.januar),
                UkjentInfotrygdperiode(21.januar, 31.januar)
            )
        )
        element.utbetalingstidslinje().also {
            assertEquals(1.januar til 20.januar, it.periode())
        }
    }

    @Test
    fun `fjerner historiske utbetalinger`() {
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 20.januar),
                ArbeidsgiverUtbetalingsperiode("ag1", 21.januar, 31.januar, 100.prosent, 25000.månedlig),
                ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar, 100.prosent, 25000.månedlig)
            )
        )
        element.fjernHistorikk(tidslinjeOf(10.NAV), "ag1").also { utbetalingstidslinje ->
            assertTrue(utbetalingstidslinje.isEmpty())
        }
        element.fjernHistorikk(tidslinjeOf(10.NAV, 10.FRI, 11.NAV, 28.NAV, 31.NAV), "ag1").also { utbetalingstidslinje ->
            assertEquals(11.januar til 31.mars, utbetalingstidslinje.periode())
            assertTrue(utbetalingstidslinje.subset(11.januar til 20.januar).all { it is Fridag })
            assertTrue(utbetalingstidslinje.subset(21.januar til 28.februar).all { it is UkjentDag })
            assertTrue(utbetalingstidslinje.subset(1.mars til 31.mars).all { it is NavDag || it is NavHelgDag })
        }
        element.fjernHistorikk(tidslinjeOf(31.UTELATE, 28.NAV, 31.NAV), "ag1").also {
            assertEquals(1.mars til 31.mars, it.periode())
        }
    }

    @Test
    fun `tar ikke med historiske fridager`() {
        val element = historikkelement(
            listOf(
                Friperiode(11.januar, 20.januar),
                ArbeidsgiverUtbetalingsperiode("ag1", 21.januar, 31.januar, 100.prosent, 25000.månedlig),
            )
        )
        element.fjernHistorikk(tidslinjeOf(10.NAV), "ag1").also { utbetalingstidslinje ->
            assertEquals(1.januar til 10.januar, utbetalingstidslinje.periode())
        }
    }

    @Test
    fun `fjerner ikke historikk fra andre arbeidsgivere`() {
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag2", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 20.januar)
            )
        )
        element.fjernHistorikk(tidslinjeOf(10.NAV, 10.FRI, 11.NAV), "ag1").also { utbetalingstidslinje ->
            assertEquals(1.januar til 31.januar, utbetalingstidslinje.periode())
            assertTrue(utbetalingstidslinje.subset(1.januar til 5.januar).all { it is NavDag })
            assertTrue(utbetalingstidslinje.subset(6.januar til 7.januar).all { it is NavHelgDag })
            assertTrue(utbetalingstidslinje.subset(8.januar til 10.januar).all { it is NavDag })
            assertTrue(utbetalingstidslinje.subset(11.januar til 20.januar).all { it is Fridag })
            assertTrue(utbetalingstidslinje.subset(21.januar til 21.januar).all { it is NavHelgDag })
            assertTrue(utbetalingstidslinje.subset(22.januar til 26.januar).all { it is NavDag })
            assertTrue(utbetalingstidslinje.subset(27.januar til 28.januar).all { it is NavHelgDag })
            assertTrue(utbetalingstidslinje.subset(29.januar til 31.januar).all { it is NavDag })
        }
    }

    @Test
    fun `sammenhengende tidslinje`() {
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig),
                Friperiode(11.januar, 12.januar),
                ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar, 100.prosent, 25000.månedlig),
                ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar, 100.prosent, 25000.månedlig)
            )
        )
        val tidslinje = element.sykdomstidslinje()
        assertTrue(tidslinje.inspektør.dager.values.none { it is Dag.UkjentDag })
        assertEquals(1.januar, tidslinje.sisteSkjæringstidspunkt())
    }

    @Test
    fun `historikk for overskriver`() {
        val sykdomstidslinje = 10.A + 5.opphold + 5.S
        val element = historikkelement(
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
        val element = historikkelement(
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
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 31.januar, 100.prosent, 25000.månedlig),
                Friperiode(1.februar, 28.februar)
            )
        )
        val tidslinje = element.historikkFor("ag1", sykdomstidslinje)
        assertEquals(sykdomstidslinje.periode(), tidslinje.periode())
    }

    @Test
    fun `ingen ukjente arbeidsgivere`() {
        val element = historikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag3", 1.januar, 9.januar, 100.prosent, 25000.månedlig),
                ArbeidsgiverUtbetalingsperiode("ag1", 10.januar, 20.januar, 100.prosent, 25000.månedlig),
                ArbeidsgiverUtbetalingsperiode("ag2", 10.januar, 20.januar, 100.prosent, 25000.månedlig),
            )
        )
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
            assertTrue(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.januar til 31.januar, 1.januar, "ag1"))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FORLENGELSE, 1.januar til 31.januar, 1.januar, "ag1"))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(element.valider(it, Periodetype.INFOTRYGDFORLENGELSE, 1.januar til 31.januar, 1.januar, "ag1"))
            assertTrue(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(element.valider(it, Periodetype.OVERGANG_FRA_IT, 1.januar til 31.januar, 1.januar, "ag1"))
            assertTrue(it.hasErrorsOrWorse())
        }
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

        val element = historikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.februar til 28.februar, 1.februar, "ag1"))
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

        val element = historikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertFalse(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `sjekker at vi har inntekt fra første dag i periodeb som har en utbetaling`() {
        val utbetalinger = listOf(
            Friperiode(1.januar, 4.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 5.januar, 1234.månedlig, true))

        val element = historikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `perioder fra infortrygd slås sammen dersom gapet kun er helg`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 8.januar, 12.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true))
        val element = historikkelement(perioder = utbetalinger, inntekter = inntekter)

        aktivitetslogg.barn().also {
            assertTrue(element.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.februar til 28.februar, 1.februar, "ag1"))
        }
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("07" to 1.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 23.januar), 1.januar, "ag1"))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering feiler ikke når det ikke er redusert utbetaling i Infotrygd, men skjæringstidspunkt i Infotrygd`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 23.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
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
        val element = historikkelement(perioder = utbetalinger, arbeidskategorikoder = arbeidskategorikoder, inntekter = inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(7.januar, 23.januar), 7.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal feile når bruker har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "07" to 6.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal ikke feile når bruker ikke har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val arbeidskategorikoder = mapOf("01" to 1.januar, "01" to 6.januar)
        val element = historikkelement(arbeidskategorikoder = arbeidskategorikoder)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `validering skal ikke feile når utbetalingshistorikken er tom`() {
        val element = historikkelement()
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(11.januar, 23.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr er ok`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode("1234", 1.januar, 3.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))

        val element = historikkelement(utbetalinger, inntekter)

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `flere inntektsopplysninger på samme orgnr er ok`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.februar, 1234.månedlig, true),
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `flere inntektsopplysninger gir ikke feil dersom de er gamle`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            inntekter = listOf(
                Inntektsopplysning("123456789", 1.januar, 1234.månedlig, true),
                Inntektsopplysning("987654321", 1.januar.minusYears(1), 1234.månedlig, true)
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(6.januar, 31.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
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

        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.juni, 30.juni), 1.april, "ag1"))
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
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, dagsats.daglig)
        )

        val inntekter = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, (dagsats * gradering).roundToInt().daglig, true),
            Inntektsopplysning(ORGNUMMER, 1.februar, dagsats.daglig, true)
        )

        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.april, 30.april), 1.april, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
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
        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.april, 30.april), 1.april, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler selv om ugyldig dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(
            perioder = utbetalinger,
            ugyldigePerioder = listOf(UgyldigPeriode(5.januar, 5.januar, 100))
        )

        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.mars, 1.mars), 1.mars, "ag1"))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            Friperiode(5.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.januar, 25.januar, 100.prosent, 1234.daglig)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(25.januar, inspektør.sistedato)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            UkjentInfotrygdperiode(5.januar, 5.januar)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val element = historikkelement()
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 1.januar), 1.januar, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val element = historikkelement(utbetalinger)
        assertFalse(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 1.januar), 1.januar, "ag1"))
        assertTrue(aktivitetslogg.hasErrorsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))

        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(28.januar, 28.januar), 28.januar, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(29.januar, 29.januar), 29.januar, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig),
            UkjentInfotrygdperiode(1.januar, 10.januar)
        )

        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.august, 1.august), 1.august, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar, 100.prosent, 1234.daglig)
        )
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 1234.daglig, true))
        val element = historikkelement(utbetalinger, inntekter)
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.august, 1.august), 1.august, "ag1"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `validering gir melding hvis vi har to inntekter for samme arbeidsgiver på samme dato`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        assertFlereInntekterInfotrygd()
    }

    @Test
    fun `validering gir ikke melding ved duplikate inntekter for samme arbeidsgiver på samme dato`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        assertEnInntektInfotrygd()
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig refusjonsinformasjon`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, false),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        assertFlereInntekterInfotrygd()
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig opphør i refusjon`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true, 15.januar),
            )
        )

        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), 1.januar, "ag1"))
        assertFlereInntekterInfotrygd()
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på forskjellig dato`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 2.januar, 1234.månedlig, true),
                Inntektsopplysning(ORGNUMMER, 1.januar, 4321.månedlig, true),
            )
        )
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.januar, 31.januar), 1.januar, "ag1"))
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
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(1.februar(2019), 28.februar(2019)), 1.februar(2019), "ag1"))
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
        assertTrue(element.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, Periode(2.januar, 31.januar), 2.januar, "ag1"))
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
        element.lagreVilkårsgrunnlag(VilkårsgrunnlagHistorikk(), sykepengegrunnlagFor(INGEN))
        assertFalse(element.kanSlettes())
    }

    @Test
    fun `element uten inntekter låses ikke`() {
        val element = historikkelement(inntekter = emptyList())
        element.addInntekter(Person("", "01010112345".somFødselsnummer(), MaskinellJurist()), aktivitetslogg)
        assertTrue(element.kanSlettes())
    }

    @Test
    fun `lagrer inntekter låser elementet`() {
        val element = historikkelement(
            inntekter = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, 1234.månedlig, true)
            )
        )
        element.addInntekter(Person("", "01010112345".somFødselsnummer(), MaskinellJurist()), aktivitetslogg)
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
        assertEquals(1234.månedlig, inntektshistorikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    private fun assertFlereInntekterInfotrygd() {
        assertTrue(aktivitetslogg.hentInfo().contains("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato."))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun assertEnInntektInfotrygd() {
        assertFalse(aktivitetslogg.hentInfo().contains("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato."))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        ugyldigePerioder: List<UgyldigPeriode> = emptyList(),
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

    private fun sykepengegrunnlagFor(inntekt: Inntekt): (LocalDate) -> Sykepengegrunnlag = {
        Sykepengegrunnlag(
            arbeidsgiverInntektsopplysninger = listOf(),
            sykepengegrunnlag = inntekt,
            grunnlagForSykepengegrunnlag = inntekt,
            begrensning = ER_IKKE_6G_BEGRENSET,
            deaktiverteArbeidsforhold = emptyList()
        )
    }
}
