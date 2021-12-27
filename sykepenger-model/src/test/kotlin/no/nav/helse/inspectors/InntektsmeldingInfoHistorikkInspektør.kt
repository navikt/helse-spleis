package no.nav.helse.inspectors

import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.InntektsmeldingInfoHistorikk
import no.nav.helse.person.InntektsmeldingInfoHistorikkVisitor
import java.time.LocalDate
import java.util.*

internal val InntektsmeldingInfoHistorikk.inspektør get() = InntektsmeldingInfoHistorikkInspektør(this)

internal class InntektsmeldingInfoHistorikkInspektør(historikk: InntektsmeldingInfoHistorikk) : InntektsmeldingInfoHistorikkVisitor {
    internal var antallElementer: Int = 0
        private set
    private val antallDatoElementer = mutableMapOf<LocalDate, Int>()

    init {
        historikk.accept(this)
    }

    internal fun antallDatoElementer(dato: LocalDate) = antallDatoElementer[dato] ?: 0

    private lateinit var sistedato: LocalDate

    override fun preVisitInntektsmeldinginfoElement(dato: LocalDate, elementer: List<InntektsmeldingInfo>) {
        this.antallElementer += 1
        this.sistedato = dato
    }

    override fun visitInntektsmeldinginfo(id: UUID, arbeidsforholdId: String?) {
        antallDatoElementer.compute(sistedato) { _, forrige -> (forrige ?: 0) + 1 }
    }
}
