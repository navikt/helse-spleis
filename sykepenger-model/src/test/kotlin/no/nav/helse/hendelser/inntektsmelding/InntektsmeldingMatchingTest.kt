package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding.FørsteDagEtterArbeidsgiverperiodenForskyvningsstragi
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding.FørsteDagEtterArbeidsgiverperiodenStrategi
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding.FørsteFraværsdagForskyvningsstragi
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding.FørsteFraværsdagStrategi
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding.HarHåndtertDagerFraInntektsmeldingenStrategi
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
import kotlin.reflect.KClass

internal class InntektsmeldingMatchingTest {

    @Test
    fun `1-16 - auu som eneste periode mottar inntektsmelding`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val (dager, inntekt) =
            inntektsmelding(1.januar, 1.januar til 16.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1))
        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `1-16, 17-31 - auu håndterer dager, forlengelse håndterer inntekt og refusjon`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val vedtaksperiode2 = 17.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(1.januar, 1.januar til 16.januar)

        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))

        assertNull(dager.håndter(vedtaksperiode2))
        assertEquals(FørsteDagEtterArbeidsgiverperiodenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `2-17, 22-31 - opplyser om annen arbeidsgiverperiode`() {
        val vedtaksperiode1 = 2.januar til 17.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(22.januar, 1.januar til 16.januar)

        assertEquals(2.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertEquals(1.januar.somPeriode(), dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på lørdag, forlengelse starter på mandag - første fraværsdag 5 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(5.januar, 5.januar til 20.januar)

        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteDagEtterArbeidsgiverperiodenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2))

        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på lørdag, forlengelse starter på mandag - første fraværsdag 22 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(22.januar, 5.januar til 20.januar)

        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2))

        // TODO: Hva første fraværsdag er satt til har innvirkning på hvor lang sykdomstisdlinjen til IM er
        assertEquals(21.januar.somPeriode(), dager.håndterGjenstående())
    }

    @Test
    fun `inntektsmelding treffer ingen vedtaksperiode`() {
        val vedtaksperiode1 = 1.januar til 31.januar
        val (dager, inntekt) =
            inntektsmelding(1.mars, 1.mars til 16.mars)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1))

        assertNull(dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
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

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1))
        assertEquals(1.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
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

        assertEquals(3.januar til 4.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertEquals(8.januar til 10.januar, dager.håndter(vedtaksperiode2))
        assertEquals(5.januar til 7.januar, dager.håndterGjenståendeFør(vedtaksperiode2))
        assertEquals(11.januar til 21.januar, dager.håndter(vedtaksperiode3))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = false))
        assertEquals(FørsteDagEtterArbeidsgiverperiodenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode3))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode4))
    }

    @Test
    fun `Første vedtaksperiode innholder første fraværsdag, andre vedtaksperiode innholder først arbeidsdag etter første fraværsdag`() {
        val vedtaksperiode1 = 1.januar til 20.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (_, inntekt) = inntektsmelding(20.januar, 1.januar til 16.januar)
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode1))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode2))
    }

    @Test
    fun `første fraværsdag i helg`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val vedtaksperiode2 = 22.januar til 31.januar

        val (dager, inntekt) =
            inntektsmelding(20.januar, 1.januar til 16.januar)

        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2))
    }

    @Test
    fun `kun en vedtaksperiode som skal håndtere inntekt og refusjon`() {
        val vedtaksperiode1 = 11.januar til 20.januar // Slutter på en lørdag (første fraværsdag)
        val vedtaksperiode2 = 22.januar til 31.januar // Startert på en mandag (første arbeidsdag etter første fraværsdag)

        val (_, inntekt) =
            inntektsmelding(20.januar, 1.januar til 16.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = true))
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

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiodeAuu, forventerInntekt = false))
        assertEquals(FørsteDagEtterArbeidsgiverperiodenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiodeMedUtbetaling, forventerInntekt = true))
    }

    @Test
    fun `Kun arbeidsgiverperiode og ferie skal ikke håndtere inntekt`() {
        val vedtaksperiode1 = 4.januar til 22.januar // Ferie 20-22.januar
        val vedtaksperiode2 = 23.januar til 31.januar

        val (_, inntekt) =
            inntektsmelding(1.januar, 4.januar til 19.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteDagEtterArbeidsgiverperiodenForskyvningsstragi::class, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = true))
    }

    @Test
    fun `Flere perioder i AUU etterfulgt av en som skal utbetales - første fraværsdag treffer i første AUU`() {
        val auu1 = 1.januar til 7.januar
        val auu2 = 8.januar til 14.januar
        val auu3 = 15.januar til 16.januar
        val skalUtbetales = 17.januar til 31.januar
        val (_, inntekt) = inntektsmelding(førsteFraværsdag = 5.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu1, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu2, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu3, forventerInntekt = false))
        assertEquals(FørsteFraværsdagForskyvningsstragi::class, inntekt.skalHåndteresAv(skalUtbetales))
    }

    @Test
    fun `Flere perioder i AUU etterfulgt av en som skal utbetales - første fraværsdag treffer i første AUU - gap til periode med utbetaling`() {
        val auu1 = 1.januar til 7.januar
        val auu2 = 8.januar til 14.januar
        val auu3 = 15.januar til 16.januar
        // Gap onsdag og torsdag 17-18.januar
        val skalUtbetales = 19.januar til 31.januar

        val (_, inntekt) = inntektsmelding(førsteFraværsdag = 5.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu1, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu2, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(auu3, forventerInntekt = false))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(skalUtbetales, forventerInntekt = true))
    }

    @Test
    fun `lang og useriøs arbeidsgiverperiode`() {
        val vedtaksperiode = 1.januar til 31.januar
        val (_, inntekt) = inntektsmelding(1.januar, 1.januar til 31.januar)

        assertEquals(FørsteDagEtterArbeidsgiverperiodenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode))
    }

    @Test
    fun `første fraværsdag i periode som ikke forventer inntekt`() {
        val vedtaksperiode1 = 4.januar til 21.januar // 20 & 21 er helg
        val vedtaksperiode2 = 22.januar til 31.januar
        val (_, inntekt) = inntektsmelding(21.januar, 4.januar til 19.januar)

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = false))
        assertEquals(FørsteFraværsdagStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = true))
    }

    @Test
    fun `arbeidsgiverperioden strekker seg utover vedtaksperioden`() {
        val vedtaksperiode1 = 1.januar til 20.januar
        val vedtaksperiode2 = 25.januar til 25.januar

        val (dager, inntekt) = inntektsmelding(25.januar, 25.januar til 25.januar.plusDays(15))
        assertNull(dager.håndter(vedtaksperiode1))
        assertEquals(25.januar til 25.januar, dager.håndter(vedtaksperiode2))

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = true))
        assertEquals(HarHåndtertDagerFraInntektsmeldingenStrategi::class, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = true))
    }
    @Test
    fun `arbeidsgiverperioden strekker seg utover vedtaksperioden, men første fraværsdag er etter perioden`() {
        val vedtaksperiode1 = 1.januar til 20.januar
        val vedtaksperiode2 = 25.januar til 25.januar

        val (dager, inntekt) = inntektsmelding(26.januar, 25.januar til 25.januar.plusDays(15))
        assertNull(dager.håndter(vedtaksperiode1))
        assertEquals(25.januar til 25.januar, dager.håndter(vedtaksperiode2))

        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode1, forventerInntekt = true))
        assertEquals(IkkeHåndtert, inntekt.skalHåndteresAv(vedtaksperiode2, forventerInntekt = true))
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

    private fun InntektOgRefusjonFraInntektsmelding.skalHåndteresAv(periode: Periode, forventerInntekt: Boolean = true): KClass<out InntektOgRefusjonMatchingstrategi> {
        // TODO: Burde vi sendt inn alle periodene i testen og prøvd seg på hver av strategiene i tur og orden på periodene
        strategier.forEach { strategi ->
            if (skalHåndteresAv(periode, strategi) { forventerInntekt }) return strategi::class
        }
        return IkkeHåndtert
    }

    private val IkkeHåndtert = IkkeHåndtertStrategi()::class

    private class IkkeHåndtertStrategi: InntektOgRefusjonMatchingstrategi {
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean) = throw IllegalStateException()
    }

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
            val dager = inntektsmelding.dager
            dager to inntektsmelding.inntektOgRefusjon(dager)
        }
    }
}