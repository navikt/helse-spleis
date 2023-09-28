package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfelleeventyrE2ETest : AbstractDslTest() {

    @Test
    fun `tre sykefraværstilfeller blir ett som følge av inntektsmelding`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 4.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(8.januar, 10.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)

        assertEquals(6, observatør.sykefraværstilfelleeventyr.size)
        val sisteEventyr = observatør.sykefraværstilfelleeventyr.last()
        assertEquals(1, sisteEventyr.size)
        val sykefraværstilfelle = sisteEventyr.single()
        assertEquals(1.januar, sykefraværstilfelle.dato)
        assertEquals(3, sykefraværstilfelle.perioder.size)
        sykefraværstilfelle.perioder[0].also { tilfelle ->
            assertEquals(1.vedtaksperiode, tilfelle.id)
            assertEquals("a1", tilfelle.organisasjonsnummer)
            assertEquals(1.januar, tilfelle.fom)
            assertEquals(4.januar, tilfelle.tom)
        }
        sykefraværstilfelle.perioder[1].also { tilfelle ->
            assertEquals(2.vedtaksperiode, tilfelle.id)
            assertEquals("a1", tilfelle.organisasjonsnummer)
            assertEquals(5.januar, tilfelle.fom)
            assertEquals(10.januar, tilfelle.tom)
        }
        sykefraværstilfelle.perioder[2].also { tilfelle ->
            assertEquals(3.vedtaksperiode, tilfelle.id)
            assertEquals("a1", tilfelle.organisasjonsnummer)
            assertEquals(11.januar, tilfelle.fom)
            assertEquals(22.januar, tilfelle.tom)
        }
    }

    @Test
    fun `ett sykefraværstilfelle blir til to pga arbeid gjenopptatt`() {
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 4.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(11.januar, 15.januar, 100.prosent), Søknad.Søknadsperiode.Arbeid(11.januar, 15.januar))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.januar, 10.januar, 100.prosent))
        }

        assertEquals(3, observatør.sykefraværstilfelleeventyr.size)
        val sisteEventyr = observatør.sykefraværstilfelleeventyr.last()
        assertEquals(2, sisteEventyr.size)

        val førsteSykefraværstilfelle = sisteEventyr[0]
        assertEquals(3.januar, førsteSykefraværstilfelle.dato)
        assertEquals(2, førsteSykefraværstilfelle.perioder.size)
        førsteSykefraværstilfelle.perioder[0].also { tilfelle ->
            assertEquals(a1 { 1.vedtaksperiode }, tilfelle.id)
            assertEquals(a1, tilfelle.organisasjonsnummer)
            assertEquals(3.januar, tilfelle.fom)
            assertEquals(4.januar, tilfelle.tom)
        }
        førsteSykefraværstilfelle.perioder[1].also { tilfelle ->
            assertEquals(a2 { 1.vedtaksperiode }, tilfelle.id)
            assertEquals(a2, tilfelle.organisasjonsnummer)
            assertEquals(5.januar, tilfelle.fom)
            assertEquals(10.januar, tilfelle.tom)
        }

        val andreSykefraværstilfelle = sisteEventyr[1]
        assertEquals(11.januar, andreSykefraværstilfelle.dato)
        assertEquals(1, andreSykefraværstilfelle.perioder.size)
        andreSykefraværstilfelle.perioder[0].also { tilfelle ->
            assertEquals(a1 { 2.vedtaksperiode }, tilfelle.id)
            assertEquals(a1, tilfelle.organisasjonsnummer)
            assertEquals(11.januar, tilfelle.fom)
            assertEquals(15.januar, tilfelle.tom)
        }
    }

    @Test
    fun `ett sykefraværstilfelle fordelt på to ag`() {
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 4.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.januar, 10.januar, 100.prosent))
        }

        assertEquals(2, observatør.sykefraværstilfelleeventyr.size)
        val sisteEventyr = observatør.sykefraværstilfelleeventyr.last()
        assertEquals(1, sisteEventyr.size)

        val sykefraværstilfelle = sisteEventyr.single()
        assertEquals(3.januar, sykefraværstilfelle.dato)
        assertEquals(2, sykefraværstilfelle.perioder.size)
        sykefraværstilfelle.perioder[0].also { tilfelle ->
            assertEquals(a1 { 1.vedtaksperiode }, tilfelle.id)
            assertEquals(a1, tilfelle.organisasjonsnummer)
            assertEquals(3.januar, tilfelle.fom)
            assertEquals(4.januar, tilfelle.tom)
        }
        sykefraværstilfelle.perioder[1].also { tilfelle ->
            assertEquals(a2 { 1.vedtaksperiode }, tilfelle.id)
            assertEquals(a2, tilfelle.organisasjonsnummer)
            assertEquals(5.januar, tilfelle.fom)
            assertEquals(10.januar, tilfelle.tom)
        }
    }
}