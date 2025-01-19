package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.dto.deserialisering.InntektshistorikkInnDto
import no.nav.helse.dto.serialisering.InntektshistorikkUtDto
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt.Companion.finnInntektsmeldingForSkjæringstidspunkt

internal class Inntektshistorikk private constructor(private val historikk: MutableList<Inntektsmeldinginntekt>) {

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal fun gjenopprett(dto: InntektshistorikkInnDto) = Inntektshistorikk(
            historikk = dto.historikk.map {
                Inntektsmeldinginntekt.gjenopprett(it)
            }.toMutableList()
        )
    }

    fun view() = InntektshistorikkView(
        inntekter = historikk.map { it.view() }
    )

    internal fun leggTil(inntekt: Inntektsmeldinginntekt): Boolean {
        if (historikk.any { !it.kanLagres(inntekt) }) return false
        historikk.add(0, inntekt)
        return true
    }

    internal fun avklarInntektsgrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
        historikk.finnInntektsmeldingForSkjæringstidspunkt(skjæringstidspunkt, førsteFraværsdag)

    internal fun dto() = InntektshistorikkUtDto(
        historikk = historikk.map { it.dto() }
    )
}

internal data class InntektshistorikkView(val inntekter: List<InntektsmeldinginntektView>)
