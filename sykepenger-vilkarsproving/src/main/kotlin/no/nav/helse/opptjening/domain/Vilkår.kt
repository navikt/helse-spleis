package no.nav.helse.opptjening.domain

internal enum class Vilkår {
    Opptjening, Medlemskap;

    val regel: Vilkårsregel
        get() = when (this) {
            Opptjening -> Opptjeningsregel
            Medlemskap -> Medlemskapsregel
        }
}

/** Grunnlag vi må innhente fra andre før et vilkår kan vurderes. */
internal enum class Grunnlagsbehov {
    Arbeidsforhold, Medlemskap
}
