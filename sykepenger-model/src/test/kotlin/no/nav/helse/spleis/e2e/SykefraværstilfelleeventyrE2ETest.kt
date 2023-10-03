package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfelleeventyrE2ETest : AbstractDslTest() {

    @Test
    fun `tre sykefraværstilfeller blir ett som følge av inntektsmelding`() {
        håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 22.januar, 100.prosent))
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
    fun `ferie mellom to perioder`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent), Ferie(21.januar, 25.januar))
            håndterSøknad(Sykdom(26.januar, 31.januar, 100.prosent))

            assertEquals(3, observatør.sykefraværstilfelleeventyr.size)
            observatør.sykefraværstilfelleeventyr.forEach { eventyr ->
                assertEquals(1.januar, eventyr.single().dato)
            }
        }
    }

    @Test
    fun `permisjon mellom to perioder`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent), Permisjon(21.januar, 25.januar))
            håndterSøknad(Sykdom(26.januar, 31.januar, 100.prosent))

            assertEquals(3, observatør.sykefraværstilfelleeventyr.size)
            observatør.sykefraværstilfelleeventyr.forEach { eventyr ->
                assertEquals(1.januar, eventyr.single().dato)
            }
        }
    }

    @Test
    fun `andre ytelser mellom to perioder`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
            håndterSøknad(Sykdom(26.januar, 31.januar, 100.prosent))
            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(21.januar, Dagtype.Foreldrepengerdag),
                ManuellOverskrivingDag(22.januar, Dagtype.Foreldrepengerdag),
                ManuellOverskrivingDag(23.januar, Dagtype.Foreldrepengerdag),
                ManuellOverskrivingDag(24.januar, Dagtype.Foreldrepengerdag),
                ManuellOverskrivingDag(25.januar, Dagtype.Foreldrepengerdag),
            ))
            assertEquals(4, observatør.sykefraværstilfelleeventyr.size)
            val siste = observatør.sykefraværstilfelleeventyr.last()
            assertEquals(2, siste.size)

            siste[0].also { eventyr ->
                assertEquals(1.januar, eventyr.dato)
                assertEquals(2, eventyr.perioder.size)
                assertEquals(1.januar til 20.januar, eventyr.perioder[0].let { it.fom til it.tom })
                assertEquals(21.januar til 25.januar, eventyr.perioder[1].let { it.fom til it.tom })
            }
            siste[1].also { eventyr ->
                assertEquals(26.januar, eventyr.dato)
                assertEquals(1, eventyr.perioder.size)
                assertEquals(26.januar til 31.januar, eventyr.perioder.single().let { it.fom til it.tom })
            }
        }
    }

    @Test
    fun `ett sykefraværstilfelle blir til to pga arbeid gjenopptatt`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
            håndterSøknad(Sykdom(11.januar, 15.januar, 100.prosent), Arbeid(11.januar, 15.januar))
        }
        a2 {
            håndterSøknad(Sykdom(5.januar, 10.januar, 100.prosent))
            håndterSøknad(Sykdom(16.januar, 20.januar, 100.prosent))
        }

        assertEquals(4, observatør.sykefraværstilfelleeventyr.size)
        val sisteEventyr = observatør.sykefraværstilfelleeventyr.last()
        assertEquals(2, sisteEventyr.size)

        sisteEventyr[0].also { førsteSykefraværstilfelle ->
            assertEquals(3.januar, førsteSykefraværstilfelle.dato)
            assertEquals(3, førsteSykefraværstilfelle.perioder.size)
            førsteSykefraværstilfelle.perioder[0].also { tilfelle ->
                assertEquals(a1 { 1.vedtaksperiode }, tilfelle.id)
                assertEquals(a1, tilfelle.organisasjonsnummer)
                assertEquals(3.januar, tilfelle.fom)
                assertEquals(4.januar, tilfelle.tom)
            }
            førsteSykefraværstilfelle.perioder[1].also { tilfelle ->
                assertEquals(a1 { 2.vedtaksperiode }, tilfelle.id)
                assertEquals(a1, tilfelle.organisasjonsnummer)
                assertEquals(11.januar, tilfelle.fom)
                assertEquals(15.januar, tilfelle.tom)
            }
            førsteSykefraværstilfelle.perioder[2].also { tilfelle ->
                assertEquals(a2 { 1.vedtaksperiode }, tilfelle.id)
                assertEquals(a2, tilfelle.organisasjonsnummer)
                assertEquals(5.januar, tilfelle.fom)
                assertEquals(10.januar, tilfelle.tom)
            }
        }

        sisteEventyr[1].also { andreSykefraværstilfelle ->
            assertEquals(16.januar, andreSykefraværstilfelle.dato)
            assertEquals(1, andreSykefraværstilfelle.perioder.size)
            andreSykefraværstilfelle.perioder[0].also { tilfelle ->
                assertEquals(a2 { 2.vedtaksperiode }, tilfelle.id)
                assertEquals(a2, tilfelle.organisasjonsnummer)
                assertEquals(16.januar, tilfelle.fom)
                assertEquals(20.januar, tilfelle.tom)
            }
        }
    }

    @Test
    fun `ett sykefraværstilfelle fordelt på to ag`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(5.januar, 10.januar, 100.prosent))
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