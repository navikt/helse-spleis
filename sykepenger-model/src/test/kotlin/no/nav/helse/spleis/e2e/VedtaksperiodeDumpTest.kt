package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.AKTØRID
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.DumpVedtaksperioder
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeDumpTest: AbstractDslTest() {
    @Test
    fun `super-enkel telletest`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
        }
        testperson.person.håndter(DumpVedtaksperioder(UUID.randomUUID(), AKTØRID, UNG_PERSON_FNR_2018.toString()))
        assertEquals(2, observatør.vedtaksperiodeDumper.size)
    }

    @Test
    fun `flere arbeidsgivere-telling`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        listOf(a1, a2).forlengVedtak(mars)
        a1 {
            forlengVedtak(april)
        }
        testperson.person.håndter(DumpVedtaksperioder(UUID.randomUUID(), AKTØRID, UNG_PERSON_FNR_2018.toString()))
        assertEquals(7, observatør.vedtaksperiodeDumper.size)
    }
}