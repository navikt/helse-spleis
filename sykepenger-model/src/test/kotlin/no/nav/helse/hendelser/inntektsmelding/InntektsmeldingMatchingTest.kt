package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.Personopplysninger
import no.nav.helse.september
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

internal class InntektsmeldingMatchingTest {

    @Test
    fun `1-16 - auu som eneste periode mottar inntektsmelding`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val (dager, inntekt) =
            inntektsmelding(1.januar, 1.januar til 16.januar)
        val strategi = inntekt.gammelStrategy


        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `1-16, 17-31 - auu håndterer dager, forlengelse håndterer inntekt og refusjon`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val vedtaksperiode2 = 17.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(1.januar, 1.januar til 16.januar)
        val strategi = inntekt.gammelStrategy


        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))

        assertNull(dager.håndter(vedtaksperiode2))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `2-17, 22-31 - opplyser om annen arbeidsgiverperiode`() {
        val vedtaksperiode1 = 2.januar til 17.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(22.januar, 1.januar til 16.januar)
        val strategi = inntekt.gammelStrategy

        assertEquals(2.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertEquals(1.januar.somPeriode(), dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på fredag, forlengelse starter på mandag - første fraværsdag 5 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(5.januar, 5.januar til 20.januar)
        val strategi = inntekt.gammelStrategy

        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på fredag, forlengelse starter på mandag - første fraværsdag 22 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(22.januar, 5.januar til 20.januar)
        val strategi = inntekt.gammelStrategy


        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))

        // TODO: Hva første fraværsdag er satt til har innvirkning på hvor lang sykdomstisdlinjen til IM er
        assertEquals(21.januar.somPeriode(), dager.håndterGjenstående())
    }

    @Test
    fun `inntektsmelding treffer ingen vedtaksperiode`() {
        val vedtaksperiode1 = 1.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(1.mars, 1.mars til 16.mars)
        val strategi = inntekt.gammelStrategy


        assertNull(dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertEquals(1.mars til 16.mars, dager.håndterGjenstående()) // Om den kalles tas alt gjenstående med uavhengig
    }

    @Test
    fun `oppstykket arbeidsgiverperiode med gjenstående dager`() {
        val vedtaksperiode1 = 1.januar til 20.januar
        val (dager, inntekt) =
            inntektsmelding(
                1.januar,
                1.januar til 5.januar, // mandag - fredag
                8.januar til 12.januar, // mandag - fredag,
                15.januar til 19.januar, // mandag - fredag,
                22.januar.somPeriode() // mandag
            )
        val strategi = inntekt.gammelStrategy

        assertEquals(1.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertEquals(21.januar til 22.januar, dager.håndterGjenstående())
    }

    @Test
    fun `vedtaksperiode som skal håndtere inntekt starter i helg`() {
        val vedtaksperiode1 = 3.januar til 4.januar
        val vedtaksperiode2 = 8.januar til 10.januar
        val vedtaksperiode3 = 11.januar til 22.januar
        val vedtaksperiode4 = 23.januar til 31.januar

        val (dager, inntekt) =
            inntektsmelding(8.januar, 3.januar til 4.januar, 8.januar til 21.januar)
        val strategi = inntekt.gammelStrategy


        assertEquals(3.januar til 4.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertEquals(8.januar til 10.januar, dager.håndter(vedtaksperiode2))
        assertEquals(5.januar til 7.januar, dager.håndterGjenståendeFør(vedtaksperiode2))
        assertEquals(11.januar til 21.januar, dager.håndter(vedtaksperiode3))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode3, strategi))
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode4, strategi))
    }

    @Test
    fun `første fraværsdag i helg`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val vedtaksperiode2 = 22.januar til 31.januar

        val (dager, inntekt) =
            inntektsmelding(20.januar, 1.januar til 16.januar)
        val strategi = inntekt.gammelStrategy


        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
    }

    @Test
    fun `kun en vedtaksperiode som skal håndtere inntekt og refusjon`() {
        val vedtaksperiode1 = 11.januar til 20.januar // Slutter på en lørdag (første fraværsdag)
        val vedtaksperiode2 = 22.januar til 31.januar // Startert på en mandag (første arbeidsdag etter første fraværsdag)

        val (_, inntekt) =
            inntektsmelding(20.januar, 1.januar til 16.januar)
        val strategi = inntekt.gammelStrategy

        assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi, forventerInntekt = false))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi, forventerInntekt = true))
    }

    @Test
    fun `vedtaksperiode som starter på mandag med arbeidsgiverperiode som slutter på foregående fredag`() {
        val vedtaksperiode1 = 21.september(2020) til 10.oktober(2020)
        val (dager, _) =
            inntektsmelding(21.september, 4.september(2020) til 19.september(2020))

        assertNull(dager.håndter(vedtaksperiode1))
        assertFalse(dager.skalHåndteresAv(vedtaksperiode1)) // håndterGjenståendeFør kalles aldri ettersom vedtaksperioden ikke håndterer noe
        assertEquals(4.september(2020) til 19.september(2020), dager.håndterGjenstående()) // Alt blir dratt med som gjenstående siden vedtaksperiode håndterer inntekt & refusjon
    }

    @Test
    fun `Har blitt håndtert av`() {
        val vedtaksperiode1 =  2.januar til 15.januar
        val (dager, _) =
            inntektsmelding(1.januar, 1.januar til 16.januar)

        assertTrue(dager.skalHåndteresAv(vedtaksperiode1))
        assertFalse(dager.harBlittHåndtertAv(vedtaksperiode1))
        assertEquals(2.januar til 15.januar, dager.håndter(vedtaksperiode1))
        assertFalse(dager.skalHåndteresAv(vedtaksperiode1))
        assertTrue(dager.harBlittHåndtertAv(vedtaksperiode1))
    }

    @Test
    fun `Poteniselle dager som skal utbetales er kun helg`() {
        val vedtaksperiodeAuu = 4.januar til 21.januar // 4-19 agp, 20-21 helg
        val vedtaksperiodeMedUtbetaling = 22.januar til 31.januar // Starter på mandag
        val (_, inntekt) =
            inntektsmelding(4.januar, 4.januar til 19.januar)

        val strategi = inntekt.gammelStrategy
        assertFalse(inntekt.skalHåndteresAv(vedtaksperiodeAuu, strategi, forventerInntekt = false))
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiodeMedUtbetaling, strategi, forventerInntekt = true))
    }

    @Test
    fun `Kun arbeidsgiverperiode og ferie skal ikke håndtere inntekt`() {
        val vedtaksperiode1 = 4.januar til 22.januar // Ferie 20-22.januar
        val vedtaksperiode2 = 23.januar til 31.januar

        val (_, inntekt) =
            inntektsmelding(1.januar, 4.januar til 19.januar)
        val strategi = inntekt.gammelStrategy


        assertForventetFeil(
            forklaring = "Vi vet ikke noe om ferie, så vi tror mandag 22.januar skal håndtere inntekt",
            nå = {
                assertTrue(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
                assertFalse(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
            },
            ønsket = {
                assertFalse(inntekt.skalHåndteresAv(vedtaksperiode1, strategi))
                assertTrue(inntekt.skalHåndteresAv(vedtaksperiode2, strategi))
            }
        )
    }

    @Test
    fun `Flere perioder i AUU etterfulgt av en som skal utbetales - første fraværsdag treffer i første AUU`() {
        val auu1 = 1.januar til 7.januar
        val auu2 = 8.januar til 14.januar
        val auu3 = 15.januar til 16.januar
        val skalUtbetales = 17.januar til 31.januar
        val (_, inntekt) = inntektsmelding(førsteFraværsdag = 5.januar)
        val strategi = inntekt.gammelStrategy


        assertFalse(inntekt.skalHåndteresAv(auu1, strategi, forventerInntekt = false))
        assertFalse(inntekt.skalHåndteresAv(auu2, strategi, forventerInntekt = false))
        assertFalse(inntekt.skalHåndteresAv(auu3, strategi, forventerInntekt = false))
        assertTrue(inntekt.skalHåndteresAv(skalUtbetales, strategi, forventerInntekt = true))
    }

    @Test
    fun `Flere perioder i AUU etterfulgt av en som skal utbetales - første fraværsdag treffer i første AUU - gap til periode med utbetaling`() {
        val auu1 = 1.januar til 7.januar
        val auu2 = 8.januar til 14.januar
        val auu3 = 15.januar til 16.januar
        // Gap onsdag og torsdag 17-18.januar
        val skalUtbetales = 19.januar til 31.januar

        val (_, inntekt) = inntektsmelding(førsteFraværsdag = 5.januar)

        val strategi = inntekt.gammelStrategy
        assertFalse(inntekt.skalHåndteresAv(auu1, strategi, forventerInntekt = false))
        assertFalse(inntekt.skalHåndteresAv(auu2, strategi, forventerInntekt = false))
        assertFalse(inntekt.skalHåndteresAv(auu3, strategi, forventerInntekt = false))
        assertFalse(inntekt.skalHåndteresAv(skalUtbetales, strategi, forventerInntekt = true))
    }

    @Test
    fun `lang og useriøs arbeidsgiverperiode`() {
        val vedtaksperiode = 1.januar til 31.januar
        val (_, inntekt) = inntektsmelding(1.januar, 1.januar til 31.januar)

        val strategi = inntekt.gammelStrategy
        assertTrue(inntekt.skalHåndteresAv(vedtaksperiode, strategi))
    }

    private fun DagerFraInntektsmelding.håndter(periode: Periode): Periode? {
        var håndtertPeriode: Periode? = null
        håndter(periode) {
            håndtertPeriode = it.sykdomstidslinje().periode()
            Sykdomstidslinje()
        }
        return håndtertPeriode
    }

    private fun DagerFraInntektsmelding.håndterGjenståendeFør(periode: Periode): Periode? {
        var håndtertPeriode: Periode? = null
        håndterGjenståendeFør(periode) {
            håndtertPeriode = it.sykdomstidslinje().periode()
        }
        return håndtertPeriode
    }

    private fun DagerFraInntektsmelding.håndterGjenstående(): Periode? {
        var håndtertPeriode: Periode? = null
        håndterGjenstående {
            håndtertPeriode = it.sykdomstidslinje().periode()
        }
        return håndtertPeriode
    }

    private fun InntektOgRefusjonFraInntektsmelding.skalHåndteresAv(periode: Periode, strategy: InntektOgRefusjonMatchingStrategy, forventerInntekt: Boolean = true) =
        skalHåndteresAv(periode, strategy) { forventerInntekt }

    private companion object {
        private fun inntektsmelding(
            førsteFraværsdag: LocalDate?,
            vararg arbeidsgiverperiode: Periode
        ) = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null),
            orgnummer = "12345678",
            fødselsnummer = "12345678910",
            aktørId = "1",
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = arbeidsgiverperiode.toList(),
            beregnetInntekt = 31000.månedlig,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harFlereInntektsmeldinger = false,
            mottatt = LocalDateTime.now(),
            personopplysninger = Personopplysninger(
                Personidentifikator.somPersonidentifikator("12345678910"),
                "1",
                LocalDate.now()
            )
        ).let { inntektsmelding ->
            inntektsmelding.dager to inntektsmelding.inntektOgRefusjon
        }
    }
}