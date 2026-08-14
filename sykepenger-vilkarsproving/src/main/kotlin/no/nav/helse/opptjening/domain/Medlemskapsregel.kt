package no.nav.helse.opptjening.domain

import java.time.LocalDate

internal object Medlemskapsregel : Vilkårsregel {
    override val vilkår = Vilkår.Medlemskap
    override val versjon = "1"

    override fun vurder(skjæringstidspunkt: LocalDate, grunnlag: Vilkårsgrunnlag): Kodeverkkode {
        check(grunnlag is Medlemskapsgrunnlag) { "Medlemskapsregelen kan ikke vurdere grunnlag for ${grunnlag.vilkår}" }
        return when (grunnlag.svar) {
            Medlemskapssvar.Ja -> Kodeverkkode.MEDLEM_I_FOLKETRYGDEN
            Medlemskapssvar.Nei -> Kodeverkkode.IKKE_MEDLEM_I_FOLKETRYGDEN
        }
    }
}
