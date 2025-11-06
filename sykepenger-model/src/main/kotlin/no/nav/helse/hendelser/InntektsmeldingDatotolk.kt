package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt
import no.nav.helse.nesteDag

internal sealed interface InntektsmeldingDatotolk {
    val refusjonsdato: LocalDate
    // Dato inntekten lagres på
    val inntektsdato: LocalDate
    // Dato for å finne perioden som skal håndtere inntekten
    val datoForHåndteringAvInntekt: LocalDate

    data class KunFørsteFraværsdag(private val førsteFraværsdag: LocalDate): InntektsmeldingDatotolk {
        override val refusjonsdato = førsteFraværsdag
        override val inntektsdato = førsteFraværsdag
        override val datoForHåndteringAvInntekt = førsteFraværsdag
    }

    data class KunArbeidsgiverperiode(private val agp: List<Periode>, private val begrunnelseForReduksjonEllerIkkeUtbetalt: Boolean): InntektsmeldingDatotolk {
        private val sisteSnute = agp.maxOf { it.start }
        private val dagSytten = agp.maxOf { it.endInclusive }.nesteDag

        override val refusjonsdato = sisteSnute
        override val inntektsdato = sisteSnute
        override val datoForHåndteringAvInntekt = when (begrunnelseForReduksjonEllerIkkeUtbetalt) {
            true -> inntektsdato
            false -> dagSytten
        }
    }

    data class Begge(val førsteFraværsdag: LocalDate, val agp: List<Periode>, private val begrunnelseForReduksjonEllerIkkeUtbetalt: Boolean): InntektsmeldingDatotolk {
        private val sisteSnute = agp.maxOf { it.start }
        private val dagSytten = agp.maxOf { it.endInclusive }.nesteDag

        override val refusjonsdato = maxOf(sisteSnute, førsteFraværsdag)
        override val inntektsdato = when (førsteFraværsdag > dagSytten) {
            true -> førsteFraværsdag
            false -> sisteSnute
        }
        override val datoForHåndteringAvInntekt = when (begrunnelseForReduksjonEllerIkkeUtbetalt) {
            true -> inntektsdato
            false -> maxOf(dagSytten, førsteFraværsdag)
        }
    }

    companion object {
        fun initialiser(førsteFraværsdag: LocalDate?, agp: List<Periode>, begrunnelseForReduksjonEllerIkkeUtbetalt: BegrunnelseForReduksjonEllerIkkeUtbetalt?) = when {
            førsteFraværsdag != null && agp.isEmpty() -> KunFørsteFraværsdag(førsteFraværsdag)
            førsteFraværsdag == null && agp.isNotEmpty() -> KunArbeidsgiverperiode(agp, begrunnelseForReduksjonEllerIkkeUtbetalt != null)
            else -> Begge(førsteFraværsdag!!, agp, begrunnelseForReduksjonEllerIkkeUtbetalt != null)
        }
    }
}
