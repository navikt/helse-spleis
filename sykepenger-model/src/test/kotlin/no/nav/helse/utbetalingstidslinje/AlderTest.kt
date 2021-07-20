package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
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

    @Test
    fun `Minimum inntekt er en halv g hvis du akkurat har fylt 67`(){
        val alder = Alder("01015149945")
        assertEquals(67, alder.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.halvG.dagsats(1.januar), alder.minimumInntekt(1.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er en dag over 67`(){
        val alder = Alder("01015149945")
        assertEquals(67, alder.alderPåDato(2.januar))
        assertEquals(Grunnbeløp.`2G`.dagsats(2.januar), alder.minimumInntekt(2.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er 69`(){
        val alder = Alder("01014949945")
        assertEquals(69, alder.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.`2G`.dagsats(1.januar), alder.minimumInntekt(1.januar), )
    }
}
