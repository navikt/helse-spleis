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

    /**
     * Brukes ved manuell saksbehandlerovertsyring. Det foreligger ingen faktabasert grunnlag —
     * saksbehandleren har selv tatt stilling til vilkåret og dokumentert begrunnelsen i fritekst.
     */
    data object ManuellOverstyring : Opptjeningsgrunnlag {
        override val besvarer = null
    }
}
