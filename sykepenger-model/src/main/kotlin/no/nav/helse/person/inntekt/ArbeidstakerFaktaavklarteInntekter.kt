package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data class ArbeidstakerFaktaavklarteInntekter(
    val førsteFraværsdag: LocalDate,
    private val vurderbareArbeidstakerFaktavklateInntekter: List<VurderbarArbeidstakerFaktaavklartInntekt>
) {
    init { check(vurderbareArbeidstakerFaktavklateInntekter.isNotEmpty()) }

    fun besteInntekt(): VurderbarArbeidstakerFaktaavklartInntekt {
        val sortert = vurderbareArbeidstakerFaktavklateInntekter.sortedBy { it.periode.start }
        val valgt = sortert.firstOrNull { førsteFraværsdag in it.periode || førsteFraværsdag > it.periode.endInclusive } ?: sortert.first()
        val harFlereFaktaavklarteInntekter = when {
            vurderbareArbeidstakerFaktavklateInntekter.any { it.harFlereFaktaavklarteInntekter } -> true
            vurderbareArbeidstakerFaktavklateInntekter.any { it.faktaavklartInntekt.id != valgt.faktaavklartInntekt.id } -> true
            else -> false
        }
        return valgt.copy(harFlereFaktaavklarteInntekter = harFlereFaktaavklarteInntekter)
    }
}

internal data class VurderbarArbeidstakerFaktaavklartInntekt(
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
    val periode: Periode,
    val harFlereFaktaavklarteInntekter: Boolean,
    private val skjæringstidspunkt: LocalDate,
    private val skjæringstidspunktVedMottattInntekt: LocalDate,
    private val nyArbeidsgiverperiodeEtterMottattInntekt: Boolean
)  {
    fun vurder(aktivitetslogg: IAktivitetslogg) {
        faktaavklartInntekt.medInnteksdato(skjæringstidspunkt).vurderVarselForGjenbrukAvInntekt(
            forrigeDato = skjæringstidspunktVedMottattInntekt,
            harNyArbeidsgiverperiode = nyArbeidsgiverperiodeEtterMottattInntekt,
            aktivitetslogg = aktivitetslogg
        )
        // TODO: if (harFlereFaktaavklarteInntekter) aktivitetslogg.varsel(Varselkode.RV_IM_4)
    }
}

