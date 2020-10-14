package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsavklaringspengerTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private val beregningsdato = 3.mars
    }

    @Test
    fun `ingen AAP`() {
        assertFalse(undersøke())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `AAP eldre enn 6 måneder`() {
        assertFalse(undersøke(Periode(
            fom = beregningsdato.minusMonths(8),
            tom = beregningsdato.minusMonths(6).minusDays(1)
        )))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `AAP innenfor 6 måneder`() {
        assertFalse(undersøke(Periode(
            fom = beregningsdato.minusMonths(8),
            tom = beregningsdato.minusMonths(6)
        )))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun undersøke(vararg perioder: Periode): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val aap = Arbeidsavklaringspenger(perioder.toList())
        return aap.valider(aktivitetslogg, beregningsdato).hasErrorsOrWorse()
    }
}
