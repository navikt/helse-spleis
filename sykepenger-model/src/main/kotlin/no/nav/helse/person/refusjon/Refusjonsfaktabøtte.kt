package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.økonomi.Inntekt

class Refusjonsfaktabøtte {
    private val refusjonsfakta = mutableListOf<Refusjonsfakta>()

    internal fun leggTil(inntektsmelding: Inntektsmelding) {
        refusjonsfakta.addAll(inntektsmelding.refusjonsfakta)
     }

    internal fun leggTil(arbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger) {
        // TODO: Legg
    }

    internal fun avklar(periode: Periode) = Refusjonstidslinje(
        periode.associateWith { dag -> refusjonsfakta.somDekker(dag).maxBy { it.tidsstempel } }.mapValues { (_, fakta) ->
            EtBeløpMedKildePåSeg(fakta.beløp, fakta.kilde)
        }
    )

    data class Refusjonsfakta(
        val fom: LocalDate,
        val tom: LocalDate?,
        val beløp: Inntekt,
        val kilde: Kilde,
        val tidsstempel: LocalDateTime
    )

    private companion object {
        fun List<Refusjonsfakta>.somDekker(dag: LocalDate): List<Refusjonsfakta> {
            return mapNotNull { fakta ->
                val periode = (fakta.fom til (fakta.tom ?: LocalDate.MAX))
                if (dag in periode) fakta
                else null
            }
        }
    }
}