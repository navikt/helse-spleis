package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderTest() {

    @Test
    fun `Kan lagre Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider ikke perioder ved duplikate sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider periode ved overlappende sykmeldingsperioder, lager ikke ny periode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(20.januar til 20.februar)
        assertEquals(listOf(1.januar til 20.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `gap fører til to sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(20.februar til 28.februar)
        assertEquals(listOf(1.januar til 31.januar, 20.februar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `to perioder med gap, kommer en periode i mellom som overlapper med begge`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 10.januar)
        sykmeldingsperioder.lagre(15.januar til 25.januar)
        assertEquals(listOf(1.januar til 10.januar, 15.januar til 25.januar), sykmeldingsperioder.perioder())

        sykmeldingsperioder.lagre(9.januar til 15.januar)
        assertEquals(listOf(1.januar til 25.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `sykmeldingsperioder lagres i riktig rekkefølge`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(15.januar til 25.januar)
        sykmeldingsperioder.lagre(5.januar til 10.januar)
        assertEquals(listOf(5.januar til 10.januar, 15.januar til 25.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `sjekk om en periode kan behandles med ett innslag`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertTrue(sykmeldingsperioder.kanFortsetteBehandling(1.desember(2017) til 31.desember(2017)))
        assertFalse(sykmeldingsperioder.kanFortsetteBehandling(1.februar til 28.februar))
    }

    @Test
    fun `sjekk om det kan behandles med flere innslag`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.februar til 28.februar)
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertTrue(sykmeldingsperioder.kanFortsetteBehandling(1.desember(2017) til 31.desember(2017)))
        assertFalse(sykmeldingsperioder.kanFortsetteBehandling(1.februar til 28.februar))
    }

    @Test
    fun `kan behandle alt ved tom sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        assertTrue(sykmeldingsperioder.kanFortsetteBehandling(LocalDate.MAX til LocalDate.MAX))
    }

    @Test
    fun `fjerner perioder frem tom søknadsperioden`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.januar til 28.februar)
        sykmeldingsperioder.fjern(1.februar til 28.februar)
        assertEquals(emptyList<Periode>(), sykmeldingsperioder.perioder())
    }

    @Test
    fun `fjerner deler av en sammenhengende sykmeldingsperiode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.februar til 28.februar)
        sykmeldingsperioder.fjern(1.januar til 31.januar)
        assertEquals(listOf(1.februar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `fjerner deler av en overlappende sykmeldingsperiode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(30.januar til 28.februar)
        sykmeldingsperioder.fjern(1.januar til 31.januar)
        assertEquals(listOf(1.februar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad kommer midt inne i en sammenhengende sykmeldingsperiode, fjerner sykmeldingsperiode tom sluttdatoen til søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.februar til 28.februar)
        sykmeldingsperioder.lagre(1.mars til 31.mars)
        sykmeldingsperioder.fjern(1.februar til 28.februar)
        assertEquals(listOf(1.mars til 31.mars), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad kommer midt inne i sykmeldingsperioder med gap, fjerner sykmeldingsperioder tom sluttdatoen til søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 25.januar)
        sykmeldingsperioder.lagre(1.februar til 25.februar)
        sykmeldingsperioder.lagre(1.mars til 25.mars)
        sykmeldingsperioder.fjern(1.februar til 25.februar)
        assertEquals(listOf(1.mars til 25.mars), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad kommer midt inne i en sykmeldingsperiode med delvis overlapp, fjerner sykmeldingsperioder tom sluttdatoen til søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 25.januar)
        sykmeldingsperioder.lagre(1.februar til 25.februar)
        sykmeldingsperioder.lagre(1.mars til 25.mars)
        sykmeldingsperioder.fjern(1.februar til 24.februar)
        assertEquals(listOf(25.februar til 25.februar, 1.mars til 25.mars), sykmeldingsperioder.perioder())
    }

    @Test
    fun `etterfølgende søknad som tilstøter sykmeldingsperiode, fjerner alt`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.fjern(1.februar til 28.februar)
        assertEquals(emptyList<Periode>(), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad forran som tilstøter sykmeldingsperioden, fjerner ingenting`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.februar til 28.februar)
        sykmeldingsperioder.fjern(1.januar til 31.januar)
        assertEquals(listOf(1.februar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `sykmeldingsperioder blir truffet eller ikke truffet riktig av inntektsmelding`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 15.januar)
        sykmeldingsperioder.lagre(1.mars til 28.mars)

        assertTrue(sykmeldingsperioder.blirTruffetAv(inntektsmelding(listOf(1.januar til 16.januar), 1.januar)))
        assertFalse(sykmeldingsperioder.blirTruffetAv(inntektsmelding(listOf(1.februar til 16.februar), 1.februar)))
        assertTrue(sykmeldingsperioder.blirTruffetAv(inntektsmelding(listOf(1.mars til 16.mars), 1.mars)))
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate
    ): Inntektsmelding =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, null),
            orgnummer = "ORGNUMMER",
            fødselsnummer = "FNR",
            aktørId = "AKTØRID",
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = Inntekt.INGEN,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harOpphørAvNaturalytelser = false,
            mottatt = LocalDateTime.now()
        )


    class Inspektør() : SykmeldingsperioderVisitor {

        val perioder = mutableListOf<Periode>()

        override fun visitSykmeldingsperiode(periode: Periode) {
            perioder.add(periode)
        }
    }

    fun Sykmeldingsperioder.perioder() = Inspektør().also(::accept).perioder
}
