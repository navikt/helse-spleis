package no.nav.helse.opptjening.domain

import no.nav.helse.februar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MedlemskapsprøvingTest {

    // Medlemskap må alltid slås opp utenfor, så prøvingen venter alltid
    @Test
    fun `prøvingen venter alltid på medlemskap`() {
        val (prøving, vurdering) = Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar)

        assertNull(vurdering)
        assertFalse(prøving.erAvsluttet)
        assertEquals(Vilkår.Medlemskap, prøving.vilkår)
        assertEquals(Grunnlagsbehov.Medlemskap, prøving.uteståendeBehov)
    }

    // Vurderingen bærer med seg prøvingen, grunnlaget og kilden – akkurat som for opptjening
    @Test
    fun `mottatt grunnlag fullfører prøvingen`() {
        val prøving = påbegyntPrøving()

        val vurdering = prøving.motta(Medlemskapsgrunnlag(Medlemskapssvar.Ja))

        assertTrue(prøving.erAvsluttet)
        assertEquals(Vilkårsprøving.Tilstand.Fullført(vurdering.id), prøving.tilstand)
        assertEquals(prøving.id, vurdering.prøvingId)
        assertEquals(Vilkår.Medlemskap, vurdering.vilkår)
        assertEquals(Medlemskapsgrunnlag(Medlemskapssvar.Ja), vurdering.grunnlag)
        assertEquals(Kilde.Automatisk(Medlemskapsregel.versjon), vurdering.kilde)
        assertEquals(Kodeverkkode.MEDLEM_I_FOLKETRYGDEN, vurdering.kodeverkkode)
        assertEquals(Utfall.Oppfylt, vurdering.utfall)
    }

    @Test
    fun `nei gir en vurdering som ikke er oppfylt`() {
        val vurdering = påbegyntPrøving().motta(Medlemskapsgrunnlag(Medlemskapssvar.Nei))

        assertEquals(Utfall.IkkeOppfylt, vurdering.utfall)
    }

    // En fullført prøving er endelig
    @Test
    fun `fullført prøving tar ikke imot mer grunnlag`() {
        val prøving = påbegyntPrøving()
        prøving.motta(Medlemskapsgrunnlag(Medlemskapssvar.Ja))

        assertThrows<IllegalStateException> { prøving.motta(Medlemskapsgrunnlag(Medlemskapssvar.Nei)) }
    }

    // Grunnlag som besvarer et annet behov skal ikke kunne fullføre prøvingen
    @Test
    fun `grunnlag som ikke besvarer behovet avvises`() {
        val prøving = påbegyntPrøving()

        assertThrows<IllegalStateException> {
            prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(emptyList()))
        }
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        fun påbegyntPrøving() = Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar).prøving
    }
}
