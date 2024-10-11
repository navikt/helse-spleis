package no.nav.helse.økonomi

import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class InntektTest {

    @Test
    fun `Like beløp, men ulike`() {
        val daglig = 2833.103538461539.daglig
        val månedlig = 61383.91.månedlig
        assertEquals(daglig.daglig, månedlig.daglig)
        assertNotEquals(daglig, månedlig)

        assertEquals(daglig.rundTilDaglig(), månedlig.rundTilDaglig())
    }
}