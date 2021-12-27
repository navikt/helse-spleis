package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import java.time.LocalDate

internal class InntektsmeldingInfoHistorikk(
    private val historikk: MutableMap<LocalDate, MutableList<InntektsmeldingInfo>> = mutableMapOf()
) {

    internal fun accept(visitor: InntektsmeldingInfoHistorikkVisitor) {
        visitor.preVisitInntektsmeldinginfoHistorikk(this)
        historikk.forEach { (key, value) ->
            visitor.preVisitInntektsmeldinginfoElement(key, value)
            value.forEach { it.accept(visitor) }
            visitor.postVisitInntektsmeldinginfoElement(key, value)
        }
        visitor.postVisitInntektsmeldinginfoHistorikk(this)
    }

    internal fun finn(dato: LocalDate) =
        historikk[dato]?.last()

    internal fun opprett(dato: LocalDate, inntektsmelding: Inntektsmelding) =
        finnEllerOpprett(dato, inntektsmelding.inntektsmeldingsinfo())

    private fun finnEllerOpprett(dato: LocalDate, inntektsmeldingInfo: InntektsmeldingInfo) =
        finn(dato, inntektsmeldingInfo) ?: inntektsmeldingInfo.also { opprett(dato, it) }

    private fun opprett(dato: LocalDate, inntektsmeldingInfo: InntektsmeldingInfo) {
        historikk.getOrPut(dato) { mutableListOf() }.add(inntektsmeldingInfo)
    }

    private fun finn(dato: LocalDate, info: InntektsmeldingInfo) =
        historikk[dato]?.firstOrNull { it == info }
}
