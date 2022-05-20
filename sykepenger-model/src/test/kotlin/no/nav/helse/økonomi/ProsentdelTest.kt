package no.nav.helse.økonomi

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.fraRatio
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ProsentdelTest {

    @Test fun equality() {
        assertEquals(fraRatio(0.25), 25.0.prosent )
        assertNotEquals(fraRatio(0.25), 75.0.prosent )
        assertNotEquals(fraRatio(0.25), Any() )
        assertNotEquals(fraRatio(0.25), null )
    }

    @Test
    fun reciprok() {
        val gradertVerdi = 5.daglig
        val grad = 50.prosent
        assertEquals(10.daglig, grad.reciproc(gradertVerdi))
        val inntekt = 100.daglig
        val enTredjedel = fraRatio(1/3.0)
        val gradertInntekt = inntekt.times(enTredjedel)
        assertEquals(inntekt, enTredjedel.reciproc(gradertInntekt))
    }

    @Test
    fun `opprette med Int`() {
        1.rangeTo(99).forEach { n ->
            assertEquals(n, n.prosent)
            assertEquals(100 - n, n.prosent.not())
        }
        assertEquals(fraRatio("0"), 0.prosent)
        assertEquals(fraRatio("1.0"), 0.prosent.not())
        assertEquals(fraRatio("1"), 100.prosent)
        assertEquals(fraRatio("0.0"), 100.prosent.not())
    }

    private fun assertEquals(n: Int, prosentdel: Prosentdel) {
        val forventet = fraRatio("0.${n.toString().padStart(2, '0').removeSuffix("0")}")
        assertEquals(forventet, prosentdel)
        assertEquals(n, prosentdel.toDouble().toInt())
    }

    @Test
    fun avrundingsfeil() {
        // Fredet variabelnavn
        val karakterMedAvrunding = (1 / 7.0).prosent
        val dobbelomvendt = !!karakterMedAvrunding
        assertEquals(karakterMedAvrunding, dobbelomvendt)
        assertEquals(karakterMedAvrunding, karakterMedAvrunding)
        assertEquals(karakterMedAvrunding.get<Double>("brøkdel"), (dobbelomvendt).get<Double>("brøkdel"))
        assertEquals(karakterMedAvrunding.hashCode(), (dobbelomvendt).hashCode())
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
