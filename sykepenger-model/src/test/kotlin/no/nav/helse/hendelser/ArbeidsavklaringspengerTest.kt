package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsavklaringspengerTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private val førsteFraværsdag = 3.mars
    }

    @Test
    internal fun `ingen AAP`() {
        assertFalse(undersøke())
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `AAP eldre enn 6 måneder`() {
        assertFalse(undersøke(Periode(
            fom = førsteFraværsdag.minusMonths(8),
            tom = førsteFraværsdag.minusMonths(6).minusDays(1)
        )))
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `AAP innenfor 6 måneder`() {
        assertFalse(undersøke(Periode(
            fom = førsteFraværsdag.minusMonths(8),
            tom = førsteFraværsdag.minusMonths(6)
        )))
        assertTrue(aktivitetslogg.hasWarnings())
    }

    private fun undersøke(vararg perioder: Periode): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val aap = Vilkårsgrunnlag.Arbeidsavklaringspenger(perioder.toList())
        return aap.valider(aktivitetslogg, førsteFraværsdag).hasErrors()
    }
}
