package no.nav.helse.hendelser

import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjening
import no.nav.helse.januar
import no.nav.helse.person.Opptjening
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagTest {

    @Test
    fun `tar ikke med arbeidsforhold som starter på skjæringstidspunktet`() {
        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold("a1", 2.januar, null, Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT)
        )
        assertEquals(emptyMap<String, List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold>>(), arbeidsforhold.opptjening(2.januar))
    }

    @Test
    fun `tar med arbeidsforhold som starter før skjæringstidspunktet`() {
        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold("a1", 2.januar, null, Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT)
        )
        assertNotEquals(emptyMap<String, List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold>>(), arbeidsforhold.opptjening(3.januar))
    }
}