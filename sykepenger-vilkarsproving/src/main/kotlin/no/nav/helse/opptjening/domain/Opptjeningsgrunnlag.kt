package no.nav.helse.opptjening.domain

/** Faktaene en opptjeningsvurdering er gjort på. Ren data — reglene ligger i [Opptjeningsregel]. */
internal sealed interface Opptjeningsgrunnlag : Vilkårsgrunnlag {
    override val vilkår get() = Vilkår.Opptjening

    data class Arbeidstaker(val arbeidsforhold: List<Arbeidsforhold>) : Opptjeningsgrunnlag {
        override val besvarer = Grunnlagsbehov.Arbeidsforhold
    }

    data object SelvstendigNæringsdrivende : Opptjeningsgrunnlag {
        override val besvarer = null
    }
}
