package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.yearMonth

internal sealed interface Inntektssituasjon {
    data class HarInntektFraArbeidsgiver(
        val inntektFraArbeidsgiver: ArbeidstakerFaktaavklartInntekt,
        private val førsteFraværsdag: LocalDate = inntektFraArbeidsgiver.inntektsdata.dato,
        private val vurderbarArbeidstakerFaktaavklartInntekt: VurderbarArbeidstakerFaktaavklartInntekt? = null
    ) : Inntektssituasjon {

        init { check(inntektFraArbeidsgiver.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) }

        internal fun avklarInntekt(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, flereArbeidsgivere: Boolean, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            check(skatteopplysning.inntektsopplysningskilde is Arbeidstakerinntektskilde.AOrdningen)
            return when (flereArbeidsgivere) {
                true -> vedFlereArbeidsgivere(skjæringstidspunkt, skatteopplysning, aktivitetslogg)
                false -> vedÉnArbeidsgiver(skjæringstidspunkt, aktivitetslogg)
            }
        }


        private fun vedFlereArbeidsgivere(skjæringstidspunkt: LocalDate, skatteopplysning: ArbeidstakerFaktaavklartInntekt, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            if (skjæringstidspunkt.yearMonth == førsteFraværsdag.yearMonth) return brukInntektFraArbeidsgiver(aktivitetslogg)
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
            return skatteopplysning
        }

        private fun vedÉnArbeidsgiver(skjæringstidspunkt: LocalDate, aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            if (skjæringstidspunkt.yearMonth == inntektFraArbeidsgiver.inntektsdata.dato.yearMonth) return brukInntektFraArbeidsgiver(aktivitetslogg)
            aktivitetslogg.varsel(Varselkode.RV_IV_14)
            return brukInntektFraArbeidsgiver(aktivitetslogg)
        }

        private fun brukInntektFraArbeidsgiver(aktivitetslogg: IAktivitetslogg): ArbeidstakerFaktaavklartInntekt {
            vurderbarArbeidstakerFaktaavklartInntekt?.vurder(aktivitetslogg)
            return inntektFraArbeidsgiver
        }

        internal companion object {
            internal fun fraArbeidstakerFaktaavklarteInntekter(arbeidstakerFaktaavklarteInntekter: ArbeidstakerFaktaavklarteInntekter) = arbeidstakerFaktaavklarteInntekter.besteInntekt().let { besteInntekt ->
                HarInntektFraArbeidsgiver(
                    inntektFraArbeidsgiver = besteInntekt.faktaavklartInntekt,
                    førsteFraværsdag = arbeidstakerFaktaavklarteInntekter.førsteFraværsdag,
                    vurderbarArbeidstakerFaktaavklartInntekt = besteInntekt
                )
            }
        }
    }
    data class GaOppÅVentePåArbeidsgiver(val periodenSomGaOpp: Vedtaksperiode): Inntektssituasjon

    data object TidligereVilkårsprøvd: Inntektssituasjon

    data object KanBehandlesUtenInntektFraArbeidsgiver: Inntektssituasjon

    data object TrengerIkkeInntektFraArbeidsgiver: Inntektssituasjon
}
