package no.nav.helse.opptjening.domain

import no.nav.helse.februar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MedlemskapsregelTest {

    // Regelen er en ren funksjon: samme grunnlag gir alltid samme kodeverkkode
    @Test
    fun `ja gir oppfylt`() {
        val kode = Medlemskapsregel.vurder(1.februar, Medlemskapsgrunnlag(Medlemskapssvar.Ja))

        assertEquals(Kodeverkkode.MEDLEM_I_FOLKETRYGDEN, kode)
        assertEquals(Utfall.Oppfylt, kode.utfall)
    }

    @Test
    fun `nei gir ikke oppfylt`() {
        val kode = Medlemskapsregel.vurder(1.februar, Medlemskapsgrunnlag(Medlemskapssvar.Nei))

        assertEquals(Kodeverkkode.IKKE_MEDLEM_I_FOLKETRYGDEN, kode)
        assertEquals(Utfall.IkkeOppfylt, kode.utfall)
    }

    // Regelen skal ikke kunne brukes på et grunnlag som hører til et annet vilkår
    @Test
    fun `grunnlag for et annet vilkår avvises`() {
        assertThrows<IllegalStateException> {
            Medlemskapsregel.vurder(1.februar, Opptjeningsgrunnlag.SelvstendigNæringsdrivende)
        }
    }
}
