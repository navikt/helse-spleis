package no.nav.helse.opptjening.domain

/**
 * Faktaene et vilkår vurderes på. Ren data — reglene ligger i [Vilkårsregel].
 *
 * Grunnlaget følger vurderingen, slik at vi i ettertid kan svare på hva vurderingen faktisk gjelder.
 */
internal sealed interface Vilkårsgrunnlag {
    val vilkår: Vilkår

    /** Behovet dette grunnlaget besvarer, eller null dersom det ikke må innhentes. */
    val besvarer: Grunnlagsbehov?
}
