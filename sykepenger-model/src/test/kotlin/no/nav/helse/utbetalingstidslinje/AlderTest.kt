package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class AlderTest {

    private companion object {
        val FYLLER_67_1_JANUAR_2018 = "01015149945".somFødselsnummer().alder()
    }

    @Test
    fun `alder på gitt dato`() {
        val alder = "12020052345".somFødselsnummer().alder()
        assertEquals(17, alder.alderPåDato(1.januar))
        assertEquals(18, alder.alderPåDato(12.februar))
    }

    @Test
    fun `Minimum inntekt er en halv g hvis du akkurat har fylt 67`() {
        assertEquals(67, FYLLER_67_1_JANUAR_2018.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.halvG.beløp(1.januar), FYLLER_67_1_JANUAR_2018.minimumInntekt(1.januar))
        assertEquals((93634 / 2).årlig, FYLLER_67_1_JANUAR_2018.minimumInntekt(1.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er en dag over 67`() {
        assertEquals(67, FYLLER_67_1_JANUAR_2018.alderPåDato(2.januar))
        assertEquals(Grunnbeløp.`2G`.beløp(2.januar), FYLLER_67_1_JANUAR_2018.minimumInntekt(2.januar))
        assertEquals((93634 * 2).årlig, FYLLER_67_1_JANUAR_2018.minimumInntekt(2.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er 69`() {
        val FYLLER_69_1_JANUAR_2018 = "01014949945".somFødselsnummer().alder()
        assertEquals(69, FYLLER_69_1_JANUAR_2018.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.`2G`.beløp(1.januar), FYLLER_69_1_JANUAR_2018.minimumInntekt(1.januar))
        assertEquals((93634 * 2).årlig, FYLLER_69_1_JANUAR_2018.minimumInntekt(1.januar))
    }

    @Test
    fun `forhøyet inntektskrav`() {
        assertFalse(FYLLER_67_1_JANUAR_2018.forhøyetInntektskrav(1.januar))
        assertTrue(FYLLER_67_1_JANUAR_2018.forhøyetInntektskrav(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder over 67`() {
        assertEquals(Begrunnelse.MinimumInntektOver67, FYLLER_67_1_JANUAR_2018.begrunnelseForMinimumInntekt(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder 67 eller under`() {
        assertEquals(Begrunnelse.MinimumInntekt, FYLLER_67_1_JANUAR_2018.begrunnelseForMinimumInntekt(1.januar))
    }
}
