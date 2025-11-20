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
        return when (vurderbareArbeidstakerFaktavklateInntekter.size > 1) {
            true -> valgt.copy(harFlereFaktaavklarteInntekter = true)
            false -> valgt
        }
    }
}

internal data class VurderbarArbeidstakerFaktaavklartInntekt(
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
    val periode: Periode,
    private val skjæringstidspunkt: LocalDate,
    private val skjæringstidspunktVedMottattInntekt: LocalDate,
    private val nyArbeidsgiverperiodeEtterMottattInntekt: Boolean,
    private val harFlereFaktaavklarteInntekter: Boolean
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

