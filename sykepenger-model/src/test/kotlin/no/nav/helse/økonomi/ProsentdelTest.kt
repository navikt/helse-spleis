package no.nav.helse.økonomi

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ProsentdelTest {

    @Test internal fun equality() {
        assertEquals(Prosentdel.fraRatio(0.25), 25.0.prosent )
        assertNotEquals(Prosentdel.fraRatio(0.25), 75.0.prosent )
        assertNotEquals(Prosentdel.fraRatio(0.25), Any() )
        assertNotEquals(Prosentdel.fraRatio(0.25), null )
    }

    @Test
    internal fun `opprette med Int`() {
        assertEquals(20.prosent, 20.0.prosent)
    }

    @Test
    internal fun avrundingsfeil() {
        val karakterMedAvrunding = (1 / 7.0).prosent
        assertEquals(karakterMedAvrunding, !!karakterMedAvrunding)
        assertNotEquals(
            karakterMedAvrunding.get<Double>("brøkdel"),
            (!!karakterMedAvrunding).get<Double>("brøkdel")
        )
        assertEquals(karakterMedAvrunding.hashCode(), (!!karakterMedAvrunding).hashCode())
    }

    @Test
    internal fun `parameterskontroll av sykdomsgrad`() {
        assertThrows<IllegalArgumentException> { (-0.001).prosent }
        assertThrows<IllegalArgumentException> { (100.001).prosent }
    }

    @Test fun minimumssyke() {
        assertFalse(25.prosent.erUnderGrensen())
        assertFalse(20.prosent.erUnderGrensen())
        assertTrue(15.prosent.erUnderGrensen())
    }
}
