package no.nav.helse

import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.mai
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GrunnbeløpTest {

    @Test
    fun dagsats() {
        assertEquals(2304, Grunnbeløp.`6G`.dagsats(1.mai(2019)))
        assertEquals(2236, Grunnbeløp.`6G`.dagsats(30.april(2019)))
    }

    @Test
    fun grunnbeløp() {
        assertEquals(599148.0, Grunnbeløp.`6G`.beløp(1.mai(2019)))
        assertEquals(581298.0, Grunnbeløp.`6G`.beløp(30.april(2019)))
    }
}
