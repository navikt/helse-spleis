package no.nav.helse.økonomi

import no.nav.helse.assertForventetFeil
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ProsentdelTest {

    @Test fun equality() {
        assertEquals(Prosentdel.fraRatio(0.25), 25.0.prosent )
        assertNotEquals(Prosentdel.fraRatio(0.25), 75.0.prosent )
        assertNotEquals(Prosentdel.fraRatio(0.25), Any() )
        assertNotEquals(Prosentdel.fraRatio(0.25), null )
    }

    @Test
    fun `opprette med Int`() {
        assertEquals(20.prosent, 20.0.prosent)
    }

    @Test
    fun avrundingsfeil() {
        // Fredet variabelnavn
        val karakterMedAvrunding = (1 / 7.0).prosent
        assertEquals(karakterMedAvrunding, !!karakterMedAvrunding)
        assertNotEquals(
            karakterMedAvrunding.get<Double>("brøkdel"),
            (!!karakterMedAvrunding).get<Double>("brøkdel")
        )
        assertEquals(karakterMedAvrunding.hashCode(), (!!karakterMedAvrunding).hashCode())
    }

    @Test
    fun `20 prosent blir ikke 19,9999`() {
        val prosent = 80.prosent.not()
        assertEquals(20.prosent, prosent)
        assertForventetFeil(
            forklaring = "motsatt 80 % er 20 %; men for en datamaskin er det veldig vanskelig å få <1 - 0.8> til å bli det",
            nå = {
                assertNotEquals(20.0, prosent.toDouble())
            },
            ønsket = {
                assertEquals(20.0, prosent.toDouble())
            })
    }

    @Test
    fun `parameterskontroll av sykdomsgrad`() {
        assertThrows<IllegalArgumentException> { (-0.001).prosent }
        assertThrows<IllegalArgumentException> { (100.001).prosent }
    }

    @Test fun minimumssyke() {
        assertFalse(25.prosent.erUnderGrensen())
        assertFalse(20.prosent.erUnderGrensen())
        assertTrue(15.prosent.erUnderGrensen())
    }
}
