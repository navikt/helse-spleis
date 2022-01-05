package no.nav.helse.utbetalingslinjer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SatstypeTest {

    @Test
    fun daglig() {
        assertEquals(Satstype.Daglig, Satstype.fromString("dag"))
        assertEquals(Satstype.Daglig, Satstype.fromString("DaG"))
        assertEquals("DAG", "${Satstype.Daglig}")
        assertEquals(1000, Satstype.Daglig.totalbeløp(500, 2))
    }

    @Test
    fun engangs() {
        assertEquals(Satstype.Engang, Satstype.fromString("eng"))
        assertEquals(Satstype.Engang, Satstype.fromString("EnG"))
        assertEquals("ENG", "${Satstype.Engang}")
        assertEquals(500, Satstype.Engang.totalbeløp(500, 2))
    }
}
