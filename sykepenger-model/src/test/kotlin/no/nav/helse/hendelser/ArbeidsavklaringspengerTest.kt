package no.nav.helse.hendelser

import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsavklaringspengerTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private val skjæringstidspunkt = 3.mars
        private val periode = skjæringstidspunkt til skjæringstidspunkt.plusDays(31)
    }

    @Test
    fun `ingen AAP`() {
        assertFalse(undersøke())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `AAP eldre enn 6 måneder`() {
        assertFalse(
            undersøke(
                Periode(
                    fom = skjæringstidspunkt.minusMonths(8),
                    tom = skjæringstidspunkt.minusMonths(6).minusDays(1)
                )
            )
        )
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `AAP innenfor 6 måneder`() {
        assertFalse(
            undersøke(
                Periode(
                    fom = skjæringstidspunkt.minusMonths(8),
                    tom = skjæringstidspunkt.minusMonths(6)
                )
            )
        )
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    private fun undersøke(vararg AAPperioder: Periode): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val aap = Arbeidsavklaringspenger(AAPperioder.toList())
        return aap.valider(aktivitetslogg, skjæringstidspunkt, periode).harFunksjonelleFeilEllerVerre()
    }
}
