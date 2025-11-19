package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.yearMonth

internal sealed interface Inntektssituasjon {
    data class HarInntektFraArbeidsgiver(val inntektFraArbeidsgiver: ArbeidstakerFaktaavklartInntekt, private val periodeMedInntektFraArbeidsgiver: Vedtaksperiode? = null) : Inntektssituasjon {
        constructor(periodeMedInntektFraArbeidsgiver: Vedtaksperiode): this(periodeMedInntektFraArbeidsgiver.behandlinger.faktaavklartInntekt!! as ArbeidstakerFaktaavklartInntekt, periodeMedInntektFraArbeidsgiver)
        init { check(inntektFraArbeidsgiver.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) }

        internal fun avklarInntekt(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, flereArbeidsgivere: Boolean, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            check(skatteopplysning.inntektsopplysningskilde is Arbeidstakerinntektskilde.AOrdningen)
            return when (flereArbeidsgivere) {
                true -> vedFlereArbeidsgivere(skjæringstidspunkt, skatteopplysning, aktivitetslogg)
                false -> vedÉnArbeidsgiver(skjæringstidspunkt, aktivitetslogg)
            }
        }

        private fun vedFlereArbeidsgivere(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            if (skjæringstidspunkt.yearMonth == inntektFraArbeidsgiver.inntektsdata.dato.yearMonth) return brukInntektFraArbeidsgiver(aktivitetslogg)
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
            return skatteopplysning
        }

        private fun vedÉnArbeidsgiver(skjæringstidspunkt: LocalDate, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            if (skjæringstidspunkt.yearMonth == inntektFraArbeidsgiver.inntektsdata.dato.yearMonth) return brukInntektFraArbeidsgiver(aktivitetslogg)
            aktivitetslogg.varsel(Varselkode.RV_IV_14)
            return brukInntektFraArbeidsgiver(aktivitetslogg)
        }

        private fun brukInntektFraArbeidsgiver(aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            periodeMedInntektFraArbeidsgiver?.behandlinger?.vurderVarselForGjenbrukAvInntekt(inntektFraArbeidsgiver, aktivitetslogg)
            return inntektFraArbeidsgiver
        }
    }
    data class GaOppÅVentePåArbeidsgiver(val periodenSomGaOpp: Vedtaksperiode): Inntektssituasjon

    data object TidligereVilkårsprøvd: Inntektssituasjon

    data object KanBehandlesUtenInntektFraArbeidsgiver: Inntektssituasjon

    data object TrengerIkkeInntektFraArbeidsgiver: Inntektssituasjon
}
