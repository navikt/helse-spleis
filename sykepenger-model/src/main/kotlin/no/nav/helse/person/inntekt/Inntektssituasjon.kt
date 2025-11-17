package no.nav.helse.person.inntekt

import no.nav.helse.person.Vedtaksperiode

internal sealed interface Inntektssitasjon {
    data class HarInntektFraArbeidsgiver(val inntektFraArbeidsgiver: ArbeidstakerFaktaavklartInntekt, val periodeMedInntektFraArbeidsgiver: Vedtaksperiode? = null): Inntektssitasjon {
        constructor(periodeMedInntektFraArbeidsgiver: Vedtaksperiode): this(periodeMedInntektFraArbeidsgiver.behandlinger.faktaavklartInntekt!! as ArbeidstakerFaktaavklartInntekt, periodeMedInntektFraArbeidsgiver)
        init { check(inntektFraArbeidsgiver.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) }
    }
    data class GaOppÅVentePåArbeidsgiver(val periodenSomGaOpp: Vedtaksperiode): Inntektssitasjon

    data object TidligereVilkårsprøvd: Inntektssitasjon

    data object KanBehandlesUtenInntektFraArbeidsgiver: Inntektssitasjon

    data object TrengerIkkeInntektFraArbeidsgiver: Inntektssitasjon
}
