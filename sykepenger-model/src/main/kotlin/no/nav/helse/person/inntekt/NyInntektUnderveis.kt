package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.dto.NyInntektUnderveisDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpstidslinje
import kotlin.collections.filterNot
import kotlin.collections.plus

data class NyInntektUnderveis(
    val orgnummer: String,
    val beløpstidslinje: Beløpstidslinje
) {
    fun dto() = NyInntektUnderveisDto(
        orgnummer = orgnummer,
        beløpstidslinje = beløpstidslinje.dto()
    )
    companion object {
        internal fun List<NyInntektUnderveis>.finnEndringsdato(
            tidligere: List<NyInntektUnderveis>
        ): LocalDate? {
            if (this == tidligere) return null
            val kjenteOrgnumreFraFør = tidligere.map { it.orgnummer }
            val nyeOrgnr = this.filter { it.orgnummer !in kjenteOrgnumreFraFør }
            if (nyeOrgnr.isNotEmpty()) return nyeOrgnr.minOfOrNull { it.beløpstidslinje.first().dato }
            return tidligere.førsteEndring(tidligere.merge(this))
        }

        private fun List<NyInntektUnderveis>.førsteEndring(others: List<NyInntektUnderveis>): LocalDate? {
            val datoer = mapNotNull { nyInntektUnderveis ->
                    val other = others.find { it.orgnummer == nyInntektUnderveis.orgnummer } ?: return@mapNotNull null
                    nyInntektUnderveis.beløpstidslinje.førsteEndring(other.beløpstidslinje)
                }
            return datoer.minOrNull()
        }

        internal fun List<NyInntektUnderveis>.erRelevantForOverstyring(skjæringstidspunkt: LocalDate, periode: Periode): Boolean {
            if (periode.start <= skjæringstidspunkt) return false
            val førsteDag = minOfOrNull { it.beløpstidslinje.first().dato } ?: return false
            val sisteDag = maxOfOrNull { it.beløpstidslinje.last().dato } ?: return false
            val omsluttendePeriode = førsteDag til sisteDag
            return omsluttendePeriode.inneholder(periode)
        }

        internal fun List<NyInntektUnderveis>.overstyr(hendelse: OverstyrArbeidsgiveropplysninger): List<NyInntektUnderveis> {
            return hendelse.overstyr(this)
        }

        internal fun List<NyInntektUnderveis>.merge(nyeInntekter: List<NyInntektUnderveis>): List<NyInntektUnderveis> {
            if (nyeInntekter.isEmpty()) return this
            val periode = nyeInntekter.minOf { it.beløpstidslinje.first().dato } til nyeInntekter.maxOf { it.beløpstidslinje.last().dato }
            return merge(periode, nyeInntekter)
        }

        fun List<NyInntektUnderveis>.merge(periode: Periode, nyeInntekter: List<NyInntektUnderveis>): List<NyInntektUnderveis> {
            val tingViHar = overskrivTilkommetInntekterForPeriode(periode, nyeInntekter)
            val kjenteOrgnumreFraFør = map { it.orgnummer }
            val nyeTing = nyeInntekter.filter { it.orgnummer !in kjenteOrgnumreFraFør }
            return (tingViHar + nyeTing).filterNot { it.beløpstidslinje.isEmpty() }
        }

        private fun List<NyInntektUnderveis>.overskrivTilkommetInntekterForPeriode(periode: Periode, nyeInntekter: List<NyInntektUnderveis>): List<NyInntektUnderveis> {
            return map { tilkommetInntekt ->
                // tom liste/null som resultat tolkes som av søknaden har fjernet inntekten i den angitte perioden
                val nyTidslinje = nyeInntekter.firstOrNull { it.orgnummer == tilkommetInntekt.orgnummer }?.beløpstidslinje ?: Beløpstidslinje()

                val tidslinjeFørPerioden = tilkommetInntekt.beløpstidslinje.tilOgMed(periode.start.forrigeDag)
                val tidslinjeEtterPerioden = tilkommetInntekt.beløpstidslinje.fraOgMed(periode.endInclusive.nesteDag)
                tilkommetInntekt.copy(beløpstidslinje = tidslinjeFørPerioden + nyTidslinje + tidslinjeEtterPerioden)
            }
        }

        fun gjenopprett(dto: NyInntektUnderveisDto) = NyInntektUnderveis(
            orgnummer = dto.orgnummer,
            beløpstidslinje = Beløpstidslinje.gjenopprett(dto.beløpstidslinje)
        )
    }
}
