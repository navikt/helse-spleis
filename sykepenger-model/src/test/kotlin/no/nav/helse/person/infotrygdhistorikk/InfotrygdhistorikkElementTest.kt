package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.august
import no.nav.helse.dto.MeldingsreferanseDto
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkelementInnDto
import no.nav.helse.februar
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkElementTest {

    internal companion object {
        private const val ORGNUMMER = "987654321"
        internal fun eksisterendeInfotrygdHistorikkelement(
            hendelseId: UUID = UUID.randomUUID(),
            oppdatert: LocalDateTime = LocalDateTime.now(),
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) =
            InfotrygdhistorikkelementInnDto(
                id = UUID.randomUUID(),
                tidsstempel = tidsstempel,
                hendelseId = MeldingsreferanseDto(hendelseId),
                ferieperioder = emptyList(),
                arbeidsgiverutbetalingsperioder = emptyList(),
                personutbetalingsperioder = emptyList(),
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
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar, 28.februar)
        )
        assertTrue(nyttHistorikkelement().funksjoneltLik(nyttHistorikkelement()))
        assertTrue(nyttHistorikkelement(perioder).funksjoneltLik(nyttHistorikkelement(perioder)))
        assertFalse(nyttHistorikkelement(perioder).funksjoneltLik(nyttHistorikkelement()))
    }

    private fun assertEquals(one: InfotrygdhistorikkElement, other: InfotrygdhistorikkElement) {
        assertTrue(one.funksjoneltLik(other))
        assertTrue(other.funksjoneltLik(one))
    }

    @Test
    fun `tidligste endring mellom - forrige og nytt element er tomt`() {
        val eksisterende = nyttHistorikkelement(perioder = emptyList())
        val nyttElement = nyttHistorikkelement(perioder = emptyList())
        assertNull(nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - nytt element er likt`() {
        var perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        )
        val eksisterende = nyttHistorikkelement(perioder = perioder)
        val nyttElement = nyttHistorikkelement(perioder = perioder)
        assertNull(nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - nytt element er tomt`() {
        val eksisterende = nyttHistorikkelement(
            perioder = listOf(
                PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
                ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
            )
        )
        val nyttElement = nyttHistorikkelement(perioder = emptyList())
        assertEquals(3.januar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - eksisterende element er tomt`() {
        val eksisterende = nyttHistorikkelement(perioder = emptyList())
        val nyttElement = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        assertEquals(3.januar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - eksisterende element fjerner periode i starten`() {
        val eksisterende = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        val nyttElement = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar)
        ))
        assertEquals(3.januar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - eksisterende element legger til periode i starten`() {
        val eksisterende = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        val nyttElement = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar)
        ))
        assertEquals(1.januar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - eksisterende element legger til periode i slutten`() {
        val eksisterende = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        val nyttElement = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.februar, 3.februar)
        ))
        assertEquals(3.februar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `tidligste endring mellom - eksisterende element endrer personutbetaling til arbeidsgiverutbetaling`() {
        val eksisterende = nyttHistorikkelement(perioder = listOf(
            PersonUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        val nyttElement = nyttHistorikkelement(perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 10.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 3.januar, 3.januar)
        ))
        assertEquals(10.januar, nyttElement.tidligsteEndringMellom(eksisterende))
    }

    @Test
    fun `person- og arbeidsgiverutbetaling på samme dag`() {
        val element1 = nyttHistorikkelement(
            perioder = listOf(
                ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar),
                PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar)
            )
        )
        val identiskElement = nyttHistorikkelement(
            perioder = listOf(
                PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar),
                ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar)
            )
        )
        assertEquals(element1, identiskElement)
        assertTrue(identiskElement.erstatter(element1))
    }

    @Test
    fun `samlet utbetalingstidslinje`() {
        val element = nyttHistorikkelement(
            listOf(
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar),
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
                ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar),
                Friperiode(11.januar, 12.januar),
                ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar),
                ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar)
            )
        )
        val tidslinje = element.sykdomstidslinje()
        assertTrue(tidslinje.inspektør.dager.values.none { it is Dag.UkjentDag })
    }

    @Test
    fun `hver første utbetalingsdag har en tilhørende inntekt`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 3.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar)
        )
        val element = nyttHistorikkelement(perioder = utbetalinger)

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, februar))
    }

    @Test
    fun `første utbetalingsdag mangler en tilhørende inntekt`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 3.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar)
        )
        val element = nyttHistorikkelement(perioder = utbetalinger)

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, februar))
    }

    @Test
    fun `sjekker at vi har inntekt fra første dag i periodeb som har en utbetaling`() {
        val utbetalinger = listOf(
            Friperiode(1.januar, 4.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 10.januar)
        )
        val element = nyttHistorikkelement(perioder = utbetalinger)

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, februar))
    }

    @Test
    fun `validering feiler ikke når det ikke er redusert utbetaling i Infotrygd, men skjæringstidspunkt i Infotrygd`() {
        val element = nyttHistorikkelement()
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(6.januar, 23.januar)))
        aktivitetslogg.assertIngenFunksjonelleFeil()
    }

    @Test
    fun `validering skal ikke feile når bruker har redusert utbetaling i Infotrygd, men skjæringstidspunkt i Spleis`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar)
        )
        val element = nyttHistorikkelement(
            perioder = utbetalinger
        )
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, 26.januar.somPeriode()))
        aktivitetslogg.assertIngenFunksjonelleFeil()
    }

    @Test
    fun `validering skal ikke feile når bruker ikke har redusert utbetaling og skjæringstidspunkt i Infotrygd  - flere arbeidsgivere`() {
        val element = nyttHistorikkelement()
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(11.januar, 23.januar)))
        aktivitetslogg.assertIngenFunksjonelleFeil()
    }

    @Test
    fun `validering skal ikke feile når utbetalingshistorikken er tom`() {
        val element = nyttHistorikkelement()
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(11.januar, 23.januar)))
        aktivitetslogg.assertIngenFunksjonelleFeil()
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med samme orgnr kastes ut`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar)
        )
        val element = nyttHistorikkelement(utbetalinger)

        assertFalse(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(6.januar, 31.januar)))
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `forlengelser fra infotrygd med tilstøtende periode med annet orgnr kastes ut`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 5.januar)
        )

        val element = nyttHistorikkelement(utbetalinger)

        assertFalse(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(6.januar, 31.januar)))
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `RefusjonTilArbeidsgiver mappes til utbetalingstidslinje`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        aktivitetslogg.assertVarsler(emptyList())

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test
    fun `RefusjonTilArbeidsgiver regnes som utbetalingsdag selv om den overlapper med ferie`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar),
            Friperiode(5.januar, 20.januar),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.januar, 25.januar)
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        aktivitetslogg.assertVarsler(emptyList())

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(25.januar, inspektør.sistedato)
        assertEquals(17, inspektør.navDagTeller)
    }

    @Test
    fun `Feiler ikke selv om ukjent dag overlappes helt av ReduksjonArbeidsgiverRefusjon`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar),
        )

        val tidslinje = utbetalinger.map { it.utbetalingstidslinje() }.reduce(Utbetalingstidslinje::plus)

        val inspektør = tidslinje.inspektør
        assertEquals(1.januar, inspektør.førstedato)
        assertEquals(10.januar, inspektør.sistedato)
    }

    @Test
    fun `Validerer ok hvis det ikke finnes noen utbetalinger fra Infotrygd`() {
        val element = nyttHistorikkelement()
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 1.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `Utbetalinger i Infotrygd som overlapper med tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar)
        )
        val element = nyttHistorikkelement(utbetalinger)
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 1.januar)))
        aktivitetslogg.assertVarsel(Varselkode.RV_IT_3)
    }

    @Test
    fun `Utbetalinger i Infotrygd som er nærmere enn 18 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar)
        )
        val element = nyttHistorikkelement(utbetalinger)
        assertFalse(element.validerMedFunksjonellFeil(aktivitetslogg, 23.januar.somPeriode()))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `Utbetalinger i Infotrygd som er eldre enn 20 dager fra tidslinjen`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 5.januar)
        )
        val element = nyttHistorikkelement(utbetalinger)
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(26.januar, 26.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `Validerer ok hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar)
        )

        val element = nyttHistorikkelement(utbetalinger)
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.august, 1.august)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `Validering ignorerer maksdato hvis perioder er eldre enn 26 uker før første fraværsdag`() {
        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 10.januar)
        )
        val element = nyttHistorikkelement(utbetalinger)
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.august, 1.august)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir melding hvis vi har to inntekter for samme arbeidsgiver på samme dato`() {
        val element = nyttHistorikkelement(
        )

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir ikke melding ved duplikate inntekter for samme arbeidsgiver på samme dato`() {
        val element = nyttHistorikkelement(
        )
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig refusjonsinformasjon`() {
        val element = nyttHistorikkelement(
        )

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir melding hvis vi har duplikate inntekter for samme arbeidsgiver på samme dato, men forskjellig opphør i refusjon`() {
        val element = nyttHistorikkelement(
        )

        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på forskjellig dato`() {
        val element = nyttHistorikkelement(
        )
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er 12 måneder før perioden`() {
        val element = nyttHistorikkelement(
        )
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(1.februar(2019), 28.februar(2019))))
        aktivitetslogg.assertVarsler(emptyList())
    }

    @Test
    fun `validering gir ikke warning hvis vi har to inntekter for samme arbeidsgiver på samme dato, men dato er før skjæringstidspunkt`() {
        val element = nyttHistorikkelement(
        )
        assertTrue(element.validerMedFunksjonellFeil(aktivitetslogg, Periode(2.januar, 31.januar)))
        aktivitetslogg.assertVarsler(emptyList())
    }

    private fun nyttHistorikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = MeldingsreferanseId(hendelseId),
            perioder = perioder
        )
}
