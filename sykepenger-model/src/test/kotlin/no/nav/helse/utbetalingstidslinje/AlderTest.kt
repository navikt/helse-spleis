package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.november
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class AlderTest {

    private companion object {
        val FYLLER_67_ÅR_1_JANUAR_2018 = "01015149945".somFødselsnummer().alder()
        val FYLLER_18_ÅR_2_NOVEMBER_2018 = "02110075045".somFødselsnummer().alder()
        val FYLLER_70_ÅR_10_JANUAR_2018 = "10014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_13_JANUAR_2018 = "13014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_14_JANUAR_2018 = "14014812345".somFødselsnummer().alder()
        val FYLLER_70_ÅR_15_JANUAR_2018 = "15014812345".somFødselsnummer().alder()
    }

    @Test
    fun `alder på gitt dato`() {
        val alder = "12029240045".somFødselsnummer().alder()
        assertEquals(25, alder.alderPåDato(11.februar))
        assertEquals(26, alder.alderPåDato(12.februar))
    }

    @Test
    fun `mindre enn 70`() {
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(9.januar))
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(10.januar))
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.innenfor70årsgrense(11.januar))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.innenfor70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_14_JANUAR_2018.innenfor70årsgrense(12.januar))
        assertTrue(FYLLER_70_ÅR_15_JANUAR_2018.innenfor70årsgrense(12.januar))
    }

    @Test
    fun `utbetaling skal stoppes selv om man reelt sett er 69 år - dersom 70årsdagen er i en helg`() {
        val dagen = 12.januar
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.innenfor70årsgrense(dagen))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.er70årsgrenseNådd(dagen))
    }

    @Test
    fun `har fylt 70 år`() {
        assertFalse(FYLLER_70_ÅR_10_JANUAR_2018.er70årsgrenseNådd(8.januar))
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.er70årsgrenseNådd(9.januar))
        assertTrue(FYLLER_70_ÅR_10_JANUAR_2018.er70årsgrenseNådd(10.januar))
    }

    @Test
    fun `har fylt 70 år hensyntar helg`() {
        assertFalse(FYLLER_70_ÅR_13_JANUAR_2018.er70årsgrenseNådd(11.januar))
        assertFalse(FYLLER_70_ÅR_14_JANUAR_2018.er70årsgrenseNådd(11.januar))
        assertFalse(FYLLER_70_ÅR_15_JANUAR_2018.er70årsgrenseNådd(11.januar))
        assertTrue(FYLLER_70_ÅR_13_JANUAR_2018.er70årsgrenseNådd(12.januar))
        assertTrue(FYLLER_70_ÅR_14_JANUAR_2018.er70årsgrenseNådd(12.januar))
        assertTrue(FYLLER_70_ÅR_15_JANUAR_2018.er70årsgrenseNådd(12.januar))
    }

    @Test
    fun `får ikke lov å søke sykepenger dersom personen er mindre enn 18 år på søknadstidspunktet`() {
        assertTrue(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(1.november))
    }

    @Test
    fun `får lov å søke dersom personen er minst 18 år`() {
        assertFalse(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(2.november))
    }
    @Test
    fun `Minimum inntekt er en halv g hvis du akkurat har fylt 67`() {
        assertEquals(67, FYLLER_67_ÅR_1_JANUAR_2018.alderPåDato(1.januar))
        assertEquals(Grunnbeløp.halvG.beløp(1.januar), FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(1.januar))
        assertEquals((93634 / 2).årlig, FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(1.januar))
    }

    @Test
    fun `Minimum inntekt er 2g hvis du er en dag over 67`() {
        assertEquals(67, FYLLER_67_ÅR_1_JANUAR_2018.alderPåDato(2.januar))
        assertEquals(Grunnbeløp.`2G`.beløp(2.januar), FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(2.januar))
        assertEquals((93634 * 2).årlig, FYLLER_67_ÅR_1_JANUAR_2018.minimumInntekt(2.januar))
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
        assertFalse(FYLLER_67_ÅR_1_JANUAR_2018.forhøyetInntektskrav(1.januar))
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.forhøyetInntektskrav(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder over 67`() {
        assertEquals(Begrunnelse.MinimumInntektOver67, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForMinimumInntekt(2.januar))
    }

    @Test
    fun `riktig begrunnelse for minimum inntekt ved alder 67 eller under`() {
        assertEquals(Begrunnelse.MinimumInntekt, FYLLER_67_ÅR_1_JANUAR_2018.begrunnelseForMinimumInntekt(1.januar))
    }
}
