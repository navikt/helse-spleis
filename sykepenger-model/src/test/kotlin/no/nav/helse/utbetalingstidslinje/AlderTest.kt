package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AlderTest {

    @Test
    fun `alder på gitt dato`() {
        val alder = Alder("12020052345")
        assertEquals(17, alder.alderPåDato(1.januar))
        assertEquals(18, alder.alderPåDato(12.februar))
    }
}
