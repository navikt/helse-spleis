package no.nav.helse.hendelser

import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DagpengerTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private val førsteFraværsdag = 3.mars
        private val periode = førsteFraværsdag til førsteFraværsdag.plusDays(30)
    }

    @Test
    fun `ingen Dagpenger`() {
        assertFalse(undersøke())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `Dagpenger eldre enn 4 uker`() {
        assertFalse(
            undersøke(
                Periode(
                    fom = førsteFraværsdag.minusMonths(8),
                    tom = førsteFraværsdag.minusWeeks(4).minusDays(1)
                )
            )
        )
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `Dagpenger innenfor 4 uker`() {
        assertFalse(
            undersøke(
                Periode(
                    fom = førsteFraværsdag.minusMonths(8),
                    tom = førsteFraværsdag.minusWeeks(4)
                )
            )
        )
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    private fun undersøke(vararg perioder: Periode): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val dagpenger = Dagpenger(perioder.toList())
        return dagpenger.valider(aktivitetslogg, førsteFraværsdag, periode).harFunksjonelleFeilEllerVerre()
    }
}
