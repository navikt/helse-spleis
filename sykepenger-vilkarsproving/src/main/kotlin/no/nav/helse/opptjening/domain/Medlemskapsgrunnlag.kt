package no.nav.helse.opptjening.domain

/**
 * Faktaene en medlemskapsvurdering er gjort på: svaret vi fikk fra medlemskapsoppslaget.
 *
 * Vi lagrer svaret vi faktisk fikk, ikke bare konklusjonen, slik at vurderingen kan forklares i ettertid.
 */
internal data class Medlemskapsgrunnlag(val svar: Medlemskapssvar) : Vilkårsgrunnlag {
    override val vilkår = Vilkår.Medlemskap
    override val besvarer = Grunnlagsbehov.Medlemskap
}

internal enum class Medlemskapssvar {
    Ja, Nei
}
