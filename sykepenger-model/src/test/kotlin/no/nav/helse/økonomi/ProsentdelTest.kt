package no.nav.helse.økonomi

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ProsentdelTest {

    @Test internal fun equality() {
        assertEquals(Prosentdel(25.0), 25.0.prosentdel )
        assertNotEquals(Prosentdel(25.0), 75.0.prosentdel )
        assertNotEquals(Prosentdel(25.0), Any() )
        assertNotEquals(Prosentdel(25.0), null )
    }

    @Test
    internal fun `opprette med Int`() {
        assertEquals(20.prosentdel, 20.0.prosentdel)
    }

    @Test
    internal fun avrundingsfeil() {
        val karakterMedAvrunding = (1 / 7.0).prosentdel
        assertEquals(karakterMedAvrunding, !!karakterMedAvrunding)
        Assertions.assertNotEquals(
            karakterMedAvrunding.get<Double>("brøkdel"),
            (!!karakterMedAvrunding).get<Double>("brøkdel")
        )
        assertEquals(karakterMedAvrunding.hashCode(), (!!karakterMedAvrunding).hashCode())
    }

    @Test
    internal fun `parameterskontroll av sykdomsgrad`() {
        assertThrows<IllegalArgumentException> { (-0.001).prosentdel }
        assertThrows<IllegalArgumentException> { (100.001).prosentdel }
    }
}

internal val Number.prosentdel get() = Prosentdel(this)
