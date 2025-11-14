package no.nav.helse.person

import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt

internal sealed interface ArbeidsgiveropplysningerSituasjon {
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt? get() = null
    val klarForVilkårsprøving: Boolean get() = true

    /** I disse situasjonene er vi klar for å vilkårsprøve, uhavgengig av om vi har inntekt og/eller refusjon **/

    data class AvklarteArbeidsgiveropplysninger(override val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt) : ArbeidsgiveropplysningerSituasjon

    data object GirOppÅVentePåArbeidsgiver : ArbeidsgiveropplysningerSituasjon

    data object TidligereVilkårsprøvd : ArbeidsgiveropplysningerSituasjon

    data object BrukerSkatteinntektPåDirekten : ArbeidsgiveropplysningerSituasjon

    data object TrengerIkkeArbeidsgiveropplysninger : ArbeidsgiveropplysningerSituasjon {
        override val klarForVilkårsprøving get() = error("Perioder innenfor arbeidsgiverperioden skal ikke vilkårsprøves")
    }

    /** I denne situasjonene er vi ikke klar for å gå videre til vikårsprøving **/

    data object ManglerArbeidsgiveropplysninger : ArbeidsgiveropplysningerSituasjon {
        override val faktaavklartInntekt get() = error("Skal ikke komme i en situasjon hvor vi henter faktaavklart inntekt fra ManglerArbeidsgiveropplysninger")
        override val klarForVilkårsprøving = false
    }
}
