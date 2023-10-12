package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmeldingVisitor

internal val DagerFraInntektsmelding.inspektør get() = DagerFraInntektsmeldingInspektør(this)
internal class DagerFraInntektsmeldingInspektør(dager: DagerFraInntektsmelding): DagerFraInntektsmeldingVisitor {
    lateinit var gjenståendeDager: Set<LocalDate>
        private set
    var periode: Periode? = null
        private set

    init {
        dager.accept(this)
    }

    override fun visitGjenståendeDager(dager: Set<LocalDate>) {
        gjenståendeDager = dager
        periode = gjenståendeDager.omsluttendePeriode
    }
}