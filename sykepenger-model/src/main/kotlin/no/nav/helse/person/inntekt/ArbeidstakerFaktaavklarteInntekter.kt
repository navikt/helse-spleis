package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7

internal data class ArbeidstakerFaktaavklarteInntekter(
    val førsteFraværsdag: LocalDate,
    private val vurderbareArbeidstakerFaktaavklarteInntekter: List<VurderbarArbeidstakerFaktaavklartInntekt>
) {
    init { check(vurderbareArbeidstakerFaktaavklarteInntekter.isNotEmpty()) }

    fun besteInntekt(): VurderbarArbeidstakerFaktaavklartInntekt {
        val sortert = vurderbareArbeidstakerFaktaavklarteInntekter.sortedBy { it.periode.start }
        return sortert.firstOrNull { førsteFraværsdag in it.periode || førsteFraværsdag > it.periode.endInclusive } ?: sortert.first()
    }
}

internal data class VurderbarArbeidstakerFaktaavklartInntekt(
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
    val periode: Periode,
    private val skjæringstidspunkt: LocalDate,
    private val tidligereSkjæringstidspunkt: LocalDate,
    private val endretArbeidsgiverperiode: Boolean
)  {
    fun vurder(aktivitetslogg: IAktivitetslogg) {
        if (tidligereSkjæringstidspunkt == skjæringstidspunkt) return
        val dagerMellom = ChronoUnit.DAYS.between(tidligereSkjæringstidspunkt, skjæringstidspunkt)

        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom tidligere skjæringstidspunkt ($tidligereSkjæringstidspunkt) og nytt skjæringstidspunkt (${skjæringstidspunkt}), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (endretArbeidsgiverperiode) {
            aktivitetslogg.info("Det er endret arbeidsgiverperiode, og dette utløser varsel om gjenbruk")
            aktivitetslogg.varsel(RV_IV_7)
        }
    }
}

