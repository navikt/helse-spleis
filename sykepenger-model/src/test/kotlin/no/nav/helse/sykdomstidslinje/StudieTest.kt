package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class StudieTest {
    @Test
    fun `studiedager`() {
        val studiedager = ConcreteSykdomstidslinje.studiedager(1.juli, 7.juli, Søknad.SøknadDagFactory)
        assertEquals(7, studiedager.length())
    }

    private val Int.juli get() = LocalDate.of(2019, Month.JULY, this)
}
