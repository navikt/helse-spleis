package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DagpengerTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private val førsteFraværsdag = 3.mars
    }

    @Test
    internal fun `ingen Dagpenger`() {
        assertFalse(undersøke())
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `Dagpenger eldre enn 4 uker`() {
        assertFalse(undersøke(Periode(
            fom = førsteFraværsdag.minusMonths(8),
            tom = førsteFraværsdag.minusWeeks(4).minusDays(1)
        )))
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `Dagpenger innenfor 4 uker`() {
        assertFalse(undersøke(Periode(
            fom = førsteFraværsdag.minusMonths(8),
            tom = førsteFraværsdag.minusWeeks(4)
        )))
        assertTrue(aktivitetslogg.hasWarnings())
    }

    private fun undersøke(vararg perioder: Periode): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val dagpenger = Dagpenger(perioder.toList())
        return dagpenger.valider(aktivitetslogg, førsteFraværsdag).hasErrors()
    }
}
