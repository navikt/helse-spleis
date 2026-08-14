package no.nav.helse.opptjening.domain

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.opptjening.domain.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OpptjeningsprøvingTest {

    // Arbeidstaker kan ikke vurderes før arbeidsforholdene er hentet inn; prøvingen venter
    @Test
    fun `arbeidstaker venter på arbeidsforhold`() {
        val (prøving, vurdering) = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker)

        assertNull(vurdering)
        assertFalse(prøving.erAvsluttet)
        assertEquals(Grunnlagsbehov.Arbeidsforhold, prøving.uteståendeBehov)
    }

    // Selvstendig næringsdrivende krever ingen innhenting, så prøvingen er ferdig med en gang
    @Test
    fun `selvstendig næringsdrivende fullføres umiddelbart`() {
        val (prøving, vurdering) = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)

        assertNotNull(vurdering)
        assertTrue(prøving.erAvsluttet)
        assertNull(prøving.uteståendeBehov)
        assertEquals(Opptjeningsprøving.Tilstand.Fullført(vurdering!!.id), prøving.tilstand)
        assertEquals(Opptjeningsgrunnlag.SelvstendigNæringsdrivende, vurdering.grunnlag)
    }

    // Vurderingen peker tilbake på prøvingen som produserte den
    @Test
    fun `vurderingen bærer med seg prøvingen, grunnlaget og kilden`() {
        val prøving = påbegyntArbeidstakerprøving()
        val arbeidsforhold = listOf(arbeidsforhold())

        val vurdering = prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(arbeidsforhold))

        assertEquals(prøving.id, vurdering.prøvingId)
        assertEquals(FØDSELSNUMMER, vurdering.fødselsnummer)
        assertEquals(1.februar, vurdering.skjæringstidspunkt)
        assertEquals(arbeidsforhold, (vurdering.grunnlag as Opptjeningsgrunnlag.Arbeidstaker).arbeidsforhold)
        assertEquals(Kilde.Automatisk(Opptjeningsregel.VERSJON), vurdering.kilde)
        assertEquals(Kodeverkkode.OPPTJENING_MINST_4_UKER, vurdering.kodeverkkode)
        assertEquals(Utfall.Oppfylt, vurdering.utfall)
    }

    // Prøvingen går til Fullført samtidig som vurderingen blir til – de to kan ikke komme i utakt
    @Test
    fun `mottatt grunnlag fullfører prøvingen`() {
        val prøving = påbegyntArbeidstakerprøving()

        val vurdering = prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(listOf(arbeidsforhold())))

        assertTrue(prøving.erAvsluttet)
        assertEquals(Opptjeningsprøving.Tilstand.Fullført(vurdering.id), prøving.tilstand)
    }

    // En fullført prøving er endelig; duplikate svar skal ikke kunne overskrive resultatet
    @Test
    fun `fullført prøving tar ikke imot mer grunnlag`() {
        val prøving = påbegyntArbeidstakerprøving()
        prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(listOf(arbeidsforhold())))

        assertThrows<IllegalStateException> {
            prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(emptyList()))
        }
    }

    // Grunnlaget må faktisk besvare behovet prøvingen venter på
    @Test
    fun `grunnlag som ikke besvarer behovet avvises`() {
        val prøving = påbegyntArbeidstakerprøving()

        assertThrows<IllegalStateException> {
            prøving.motta(Opptjeningsgrunnlag.SelvstendigNæringsdrivende)
        }
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ORGNUMMER = "987654321"

        fun arbeidsforhold() = Arbeidsforhold(orgnummer = ORGNUMMER, ansettelseperiode = 1.januar til 31.januar, type = ORDINÆRT)

        fun påbegyntArbeidstakerprøving() =
            Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker).prøving
    }
}
