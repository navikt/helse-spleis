package no.nav.helse.person.inntekt

import no.nav.helse.person.Vedtaksperiode

internal sealed interface Inntektssituasjon {
    data class HarInntektFraArbeidsgiver(val inntektFraArbeidsgiver: ArbeidstakerFaktaavklartInntekt, val periodeMedInntektFraArbeidsgiver: Vedtaksperiode? = null): Inntektssituasjon {
        constructor(periodeMedInntektFraArbeidsgiver: Vedtaksperiode): this(periodeMedInntektFraArbeidsgiver.behandlinger.faktaavklartInntekt!! as ArbeidstakerFaktaavklartInntekt, periodeMedInntektFraArbeidsgiver)
        init { check(inntektFraArbeidsgiver.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) }
    }
    data class GaOppÅVentePåArbeidsgiver(val periodenSomGaOpp: Vedtaksperiode): Inntektssituasjon

    data object TidligereVilkårsprøvd: Inntektssituasjon

    data object KanBehandlesUtenInntektFraArbeidsgiver: Inntektssituasjon

    data object TrengerIkkeInntektFraArbeidsgiver: Inntektssituasjon
}
