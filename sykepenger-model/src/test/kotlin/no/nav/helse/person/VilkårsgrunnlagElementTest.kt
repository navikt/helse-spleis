package no.nav.helse.person

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class VilkårsgrunnlagElementTest {

    @Test
    fun `infotrygd har ikke avviksjekk`() {
        val element = infotrygdgrunnlag()
        assertTrue(element.validerAvviksprosent())
    }

    @Test
    fun `null avvik er ok`() {
        val element = grunnlagsdata(avviksprosent = null)
        assertTrue(element.validerAvviksprosent())
    }

    @Test
    fun `litt avvik er lov`() {
        val element = grunnlagsdata(avviksprosent = Prosent.ratio(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.ratio() - 0.0001))
        assertTrue(element.validerAvviksprosent())
    }

    @Test
    fun `avvik er ikke lov`() {
        val element = grunnlagsdata(avviksprosent = Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT)
        assertFalse(element.validerAvviksprosent())
    }

    @Test
    fun `for mye avvik er ikke lov`() {
        val element = grunnlagsdata(avviksprosent = Prosent.ratio(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.ratio() + 0.0001))
        assertFalse(element.validerAvviksprosent())
    }

    private fun grunnlagsdata(avviksprosent: Prosent? = null): VilkårsgrunnlagHistorikk.Grunnlagsdata {
        return VilkårsgrunnlagHistorikk.Grunnlagsdata(Sykepengegrunnlag(1000.daglig, emptyList(), 1000.daglig, Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET), 1000.daglig, avviksprosent, 0, true, Medlemskapsvurdering.Medlemskapstatus.Ja, true, true, UUID.randomUUID())
    }

    private fun infotrygdgrunnlag() =
        VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(Sykepengegrunnlag(1000.daglig, emptyList(), 1000.daglig, Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET))
}
