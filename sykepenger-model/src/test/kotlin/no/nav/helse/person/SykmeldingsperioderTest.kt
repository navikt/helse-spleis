package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderTest {

    private val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
        organisasjonsnummer = "ORGNUMMER",
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER")
    )

    private fun Sykmeldingsperioder.lagre(periode: Periode) =
        lagre(hendelsefabrikk.lagSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive)), Aktivitetslogg())

    @Test
    fun `Kan lagre Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        assertEquals(listOf(januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider ikke perioder ved duplikate sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(januar)
        assertEquals(listOf(januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider periode ved overlappende sykmeldingsperioder, lager ikke ny periode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(20.januar til 20.februar)
        assertEquals(listOf(1.januar til 20.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `gap fører til to sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(20.februar til 28.februar)
        assertEquals(listOf(januar, 20.februar til 28.februar), sykmeldingsperioder.perioder())
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
        sykmeldingsperioder.lagre(januar)
        assertFalse(sykmeldingsperioder.avventerSøknad(1.desember(2017) til 31.desember(2017)))
        assertTrue(sykmeldingsperioder.avventerSøknad(1.desember(2017) til 1.januar))
        assertFalse(sykmeldingsperioder.avventerSøknad(februar))
    }

    @Test
    fun `sjekk om det kan behandles med flere innslag`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(februar)
        sykmeldingsperioder.lagre(januar)
        assertFalse(sykmeldingsperioder.avventerSøknad(1.desember(2017) til 31.desember(2017)))
        assertTrue(sykmeldingsperioder.avventerSøknad(februar))
    }

    @Test
    fun `kan behandle alt ved tom sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        assertFalse(sykmeldingsperioder.avventerSøknad(LocalDate.MAX til LocalDate.MAX))
    }

    @Test
    fun `fjerner perioder frem tom søknadsperioden`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(1.januar til 28.februar)
        sykmeldingsperioder.fjern(februar)
        assertEquals(emptyList<Periode>(), sykmeldingsperioder.perioder())
    }

    @Test
    fun `fjerner deler av en sammenhengende sykmeldingsperiode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 28.februar)
        sykmeldingsperioder.fjern(januar)
        assertEquals(listOf(februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `fjerner deler av en overlappende sykmeldingsperiode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(30.januar til 28.februar)
        sykmeldingsperioder.fjern(januar)
        assertEquals(listOf(februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad kommer midt inne i en sammenhengende sykmeldingsperiode, fjerner sykmeldingsperiode tom sluttdatoen til søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.lagre(februar)
        sykmeldingsperioder.lagre(mars)
        sykmeldingsperioder.fjern(1.januar til 28.februar)
        assertEquals(listOf(mars), sykmeldingsperioder.perioder())
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
    fun `bevarer sykmeldingsperiode som kommer etter søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 28.februar)
        sykmeldingsperioder.fjern(2.januar til 20.januar)
        assertEquals(listOf(21.januar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad kommer midt inne i en sykmeldingsperiode med delvis overlapp, fjerner sykmeldingsperioder tom sluttdatoen til søknad`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 25.januar)
        sykmeldingsperioder.lagre(1.februar til 25.februar)
        sykmeldingsperioder.lagre(1.mars til 25.mars)
        sykmeldingsperioder.fjern(5.januar til 24.februar)
        assertEquals(listOf(25.februar til 25.februar, 1.mars til 25.mars), sykmeldingsperioder.perioder())
    }

    @Test
    fun `etterfølgende søknad som tilstøter sykmeldingsperiode, fjerner alt`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(januar)
        sykmeldingsperioder.fjern(februar)
        assertEquals(emptyList<Periode>(), sykmeldingsperioder.perioder())
    }

    @Test
    fun `søknad forran som tilstøter sykmeldingsperioden, fjerner ingenting`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(februar)
        sykmeldingsperioder.fjern(januar)
        assertEquals(listOf(februar), sykmeldingsperioder.perioder())
    }
}
