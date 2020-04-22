package no.nav.helse.sykdomstidslinje

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GradTest {

    @Test
    internal fun `opprette med Int`() {
        assertEquals(Grad.sykdom(20.0), Grad.sykdom(20))
    }

    @Test
    internal fun `arbeid helse og sykdom karakter er motsetninger`() {
        assertEquals(Grad.sykdom(25), Grad.arbeidshelse(75))
    }

    @Test
    internal fun avrundingsfeil() {
        val karakterMedAvrunding = Grad.sykdom(1 / 7.0)
        assertEquals(karakterMedAvrunding, !!karakterMedAvrunding)
        assertNotEquals(
            karakterMedAvrunding.get<Double>("brøkdel"),
            (!!karakterMedAvrunding).get<Double>("brøkdel")
        )
        assertEquals(karakterMedAvrunding.hashCode(), (!!karakterMedAvrunding).hashCode())
    }

    @Test
    internal fun minimumssyke() {
        assertTrue(Grad.sykdom(25) > Grad.GRENSE)
        assertTrue(Grad.sykdom(20) == Grad.GRENSE)
        assertTrue(Grad.sykdom(15) < Grad.GRENSE)
    }
}
