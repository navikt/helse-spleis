package no.nav.helse.person.inntekt

import no.nav.helse.dto.NyInntektUnderveisDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
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
