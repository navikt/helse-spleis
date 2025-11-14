package no.nav.helse.person

import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt

internal sealed interface ArbeidsgiveropplysningerSituasjon {
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt? get() = null
    val klarForVilkårsprøving: Boolean

    /** I Disse situasjonene er vi klar for å vilkårsprøve, uhavgengig av om vi har inntekt og/eller refusjon **/

    data class AvklartInntektOgRefusjon(override val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt) : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = true
    }

    data object GirOppÅVentePåArbeidsgiver : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = true
    }

    data object TidligereVilkårsprøvd : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = true
    }

    data object BrukerSkatteinntektPåDirekten : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = true
    }

    /** I Disse situasjonene er vi ikke klar for å gå videre til vikårsprøving **/

    data object ManglerInntektOgRefusjon : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = false
    }

    data class ManglerRefusjon(override val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt) : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving = false
    }
}
