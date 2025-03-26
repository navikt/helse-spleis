package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.forrigeDag
import no.nav.helse.januar
import no.nav.helse.nesteDag
import no.nav.helse.november
import no.nav.helse.september
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AlderTest {

    private companion object {
        val FYLLER_67_ÅR_1_JANUAR_2018 = 1.januar(1951).alder
        val FYLLER_18_ÅR_2_NOVEMBER_2018 = 2.november(2000).alder
    }

    @Test
    fun `alder på gitt dato`() {
        val alder = 12.februar(1992).alder
        assertEquals(25, alder.alderPåDato(11.februar))
        assertEquals(26, alder.alderPåDato(12.februar))
    }

    @Test
    fun `alder etter død`() {
        val dødsdato = 15.september(2022)
        val alder = Alder(16.september(1992), dødsdato)
        assertEquals(29, alder.alderPåDato(dødsdato.forrigeDag))
        assertEquals(29, alder.alderPåDato(dødsdato.nesteDag))
    }

    @Test
    fun `67årsgrense`() {
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(31.desember(2017)))
        assertTrue(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(1.januar))
        assertFalse(FYLLER_67_ÅR_1_JANUAR_2018.innenfor67årsgrense(2.januar))
    }

    @Test
    fun `får ikke lov å søke sykepenger dersom personen er mindre enn 18 år på søknadstidspunktet`() {
        assertTrue(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(1.november))
    }

    @Test
    fun `får lov å søke dersom personen er minst 18 år`() {
        assertFalse(FYLLER_18_ÅR_2_NOVEMBER_2018.forUngForÅSøke(2.november))
    }
}
