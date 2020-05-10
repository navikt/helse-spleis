package no.nav.helse.økonomi

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.sykdomstidslinje.Grad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GradTest {

    @Test
    internal fun `opprette med Int`() {
        assertEquals(
            Grad.sykdom(20.0),
            Grad.sykdom(20)
        )
    }

    @Test
    internal fun `arbeid helse og sykdom karakter er motsetninger`() {
        assertEquals(
            Grad.sykdom(25),
            Grad.arbeidshelse(75)
        )
    }

    @Test
    internal fun avrundingsfeil() {
        val karakterMedAvrunding = Grad.sykdom(1 / 7.0)
        assertEquals(karakterMedAvrunding, !!karakterMedAvrunding)
        assertNotEquals(
            karakterMedAvrunding.get<Prosentdel>("prosentdel").get<Double>("brøkdel"),
            (!!karakterMedAvrunding).get<Prosentdel>("prosentdel").get<Double>("brøkdel")
        )
        assertEquals(karakterMedAvrunding.hashCode(), (!!karakterMedAvrunding).hashCode())
    }

    @Test
    internal fun minimumssyke() {
        assertTrue(Grad.sykdom(25) > Grad.GRENSE)
        assertTrue(Grad.sykdom(20) == Grad.GRENSE)
        assertTrue(Grad.sykdom(15) < Grad.GRENSE)
    }

    @Test
    internal fun `parameterskontroll av sykdomsgrad`() {
        assertThrows<IllegalArgumentException> {
            Grad.sykdom(
                -0.001
            )
        }
        assertThrows<IllegalArgumentException> {
            Grad.arbeidshelse(
                100.001
            )
        }
    }
}
