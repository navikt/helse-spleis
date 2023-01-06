package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Personopplysninger
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
        val (dager, inntektOgRefusjon) =
            inntektsmelding(1.januar, 1.januar til 16.januar)

        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `1-16, 17-31 - auu håndterer dager, forlengelse håndterer inntekt og refusjon`() {
        val vedtaksperiode1 = 1.januar til 16.januar
        val vedtaksperiode2 = 17.januar til 31.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(1.januar, 1.januar til 16.januar)

        assertEquals(1.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))

        assertNull(dager.håndter(vedtaksperiode2))
        assertTrue(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `2-17, 22-31 - opplyser om annen arbeidsgiverperiode`() {
        val vedtaksperiode1 = 2.januar til 17.januar
        val vedtaksperiode2 = 22.januar til 31.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(22.januar, 1.januar til 16.januar)

        assertEquals(2.januar til 16.januar, dager.håndter(vedtaksperiode1))
        assertEquals(1.januar.somPeriode(), dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertTrue(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på fredag, forlengelse starter på mandag - første fraværsdag 5 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 23.januar til 31.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(5.januar, 5.januar til 20.januar)

        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertTrue(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode2))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `arbeidsgiverperiode slutter på fredag, forlengelse starter på mandag - første fraværsdag 23 januar`() {
        val vedtaksperiode1 = 5.januar til 20.januar
        val vedtaksperiode2 = 23.januar til 31.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(23.januar, 5.januar til 20.januar)

        assertEquals(5.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertNull(dager.håndter(vedtaksperiode2))

        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertTrue(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode2))

        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `inntektsmelding treffer ingen vedtaksperiode`() {
        val vedtaksperiode1 = 1.januar til 31.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(1.mars, 1.mars til 16.mars)

        assertNull(dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertNull(dager.håndterGjenstående())
    }

    @Test
    fun `oppstykket arbeidsgiverperiode med gjenstående dager`() {
        val vedtaksperiode1 = 1.januar til 20.januar
        val (dager, inntektOgRefusjon) =
            inntektsmelding(1.januar,
                1.januar til 5.januar, // mandag - fredag
                8.januar til 12.januar, // mandag - fredag,
                15.januar til 19.januar, // mandag - fredag,
                22.januar.somPeriode() // mandag
            )

        assertEquals(1.januar til 20.januar, dager.håndter(vedtaksperiode1))
        assertNull(dager.håndterGjenståendeFør(vedtaksperiode1))
        assertFalse(inntektOgRefusjon.skalHåndteresAv(vedtaksperiode1))
        assertEquals(21.januar til 22.januar, dager.håndterGjenstående())
    }

    private fun DagerFraInntektsmelding.håndter(periode: Periode): Periode? {
        var håndtertPeriode: Periode? = null
        håndter(periode) {
            håndtertPeriode = it.sykdomstidslinje().periode()
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
            personopplysninger = Personopplysninger(Personidentifikator.somPersonidentifikator("12345678910"), "1", LocalDate.now(), emptyList())
        ).let { inntektsmelding ->
            inntektsmelding.dager to inntektsmelding.inntektOgRefusjon
        }
    }
}