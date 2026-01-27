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
        val perioderDetSkalFattesVedtakPå = sortert.filter { it.skalFattesVedtakPåPerioden }

        // Finner perioden nærmest første fraværsdag som det skal fattes vedtak på
        val periodeDetSkalFattesVedtakPåNærmestFørsteFraværsdag = perioderDetSkalFattesVedtakPå
            .firstOrNull { it.periode.start >= førsteFraværsdag }
            ?: return perioderDetSkalFattesVedtakPå.firstOrNull() ?: sortert.first()

        // Utvalget vi nå skal velge inntekter blandt er alle perioder frem til og med periodeDetSkalFattesVedtakPåNærmestFørsteFraværsdag
        // Det vil si at vi får med oss alle AUU'er i forkant som eventuelt kan ha inntekt lagret på seg
        val utvalg = sortert
            .filter { it.periode.start <= periodeDetSkalFattesVedtakPåNærmestFørsteFraværsdag.periode.start}

        // Av utvalget vi nå sitter igjen med velger vi den sist ankomne
        return utvalg.maxBy { it.faktaavklartInntekt.inntektsdata.tidsstempel }
    }
}

internal data class VurderbarArbeidstakerFaktaavklartInntekt(
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
    val periode: Periode,
    val skalFattesVedtakPåPerioden: Boolean,
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

