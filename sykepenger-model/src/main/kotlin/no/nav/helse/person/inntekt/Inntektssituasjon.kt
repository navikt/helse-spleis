package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.yearMonth

internal sealed interface Inntektssituasjon {
    data class HarInntektFraArbeidsgiver(private val arbeidstakerFaktaavklarteInntekter: ArbeidstakerFaktaavklarteInntekter) : Inntektssituasjon {
        private val vurderbarArbeidstakerFaktaavklartInntekt = arbeidstakerFaktaavklarteInntekter.besteInntekt()
        private val førsteFraværsdag = arbeidstakerFaktaavklarteInntekter.førsteFraværsdag
        private val inntektFraArbeidsgiver = vurderbarArbeidstakerFaktaavklartInntekt.faktaavklartInntekt

        init { check(inntektFraArbeidsgiver.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) }

        internal fun avklarInntekt(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, flereArbeidsgivere: Boolean, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            check(skatteopplysning.inntektsopplysningskilde is Arbeidstakerinntektskilde.AOrdningen)
            return when (flereArbeidsgivere) {
                true -> vedFlereArbeidsgivere(skjæringstidspunkt, skatteopplysning, aktivitetslogg)
                false -> brukInntektFraArbeidsgiver(aktivitetslogg)
            }
        }

        private fun vedFlereArbeidsgivere(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            if (skjæringstidspunkt.yearMonth == førsteFraværsdag.yearMonth) return brukInntektFraArbeidsgiver(aktivitetslogg)
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
            return skatteopplysning
        }

        private fun brukInntektFraArbeidsgiver(aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            vurderbarArbeidstakerFaktaavklartInntekt.vurder(aktivitetslogg)
            return inntektFraArbeidsgiver
        }
    }

    data class GaOppÅVentePåArbeidsgiver(val periodenSomGaOpp: Vedtaksperiode): Inntektssituasjon

    data object TidligereVilkårsprøvd: Inntektssituasjon

    data object KanBehandlesUtenInntektFraArbeidsgiver: Inntektssituasjon

    data object TrengerIkkeInntektFraArbeidsgiver: Inntektssituasjon
}
