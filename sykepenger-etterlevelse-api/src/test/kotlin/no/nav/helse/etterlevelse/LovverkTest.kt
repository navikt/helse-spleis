package no.nav.helse.etterlevelse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LovverkTest {
    @Test
    fun tostring() {
        assertEquals("folketrygdloven", folketrygdloven.toString())
        assertEquals("folketrygdloven § 8-2 2. ledd 1. punktum bokstav c", folketrygdloven.paragraf(Paragraf.PARAGRAF_8_2).annetLedd.førstePunktum.bokstavC.toString())
    }
}