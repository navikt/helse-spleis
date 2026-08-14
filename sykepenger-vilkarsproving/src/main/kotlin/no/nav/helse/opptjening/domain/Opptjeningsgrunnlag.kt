package no.nav.helse.opptjening.domain

/**
 * Faktaene en opptjeningsvurdering er gjort på. Ren data — reglene ligger i [Opptjeningsregel].
 *
 * Grunnlaget følger vurderingen, slik at vi i ettertid kan svare på hva vurderingen faktisk gjelder.
 */
internal sealed interface Opptjeningsgrunnlag {
    /** Behovet dette grunnlaget besvarer, eller null dersom det ikke må innhentes. */
    val besvarer: Grunnlagsbehov?

    data class Arbeidstaker(val arbeidsforhold: List<Arbeidsforhold>) : Opptjeningsgrunnlag {
        override val besvarer = Grunnlagsbehov.Arbeidsforhold
    }

    data object SelvstendigNæringsdrivende : Opptjeningsgrunnlag {
        override val besvarer = null
    }
}

internal enum class Grunnlagsbehov {
    Arbeidsforhold
}
