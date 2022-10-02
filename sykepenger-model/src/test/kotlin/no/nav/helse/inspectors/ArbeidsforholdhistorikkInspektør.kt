package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.ArbeidsforholdhistorikkVisitor

internal val Arbeidsforholdhistorikk.inspektør get() = ArbeidsforholdhistorikkInspektør(this)

internal class ArbeidsforholdhistorikkInspektør(historikk: Arbeidsforholdhistorikk) : ArbeidsforholdhistorikkVisitor {

    private val innslag = mutableListOf<UUID>()
    private val arbeidsforhold = mutableMapOf<UUID, Int>()

    init {
        historikk.accept(this)
    }

    internal fun antallInnslag() = innslag.size
    internal fun arbeidsforholdSisteInnslag() = arbeidsforholdForInnslag(innslag.first())
    internal fun arbeidsforholdForInnslag(id: UUID) = arbeidsforhold.getValue(id)

    override fun preVisitArbeidsforholdinnslag(
        arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag,
        id: UUID,
        skjæringstidspunkt: LocalDate
    ) {
        innslag.add(id)
        arbeidsforhold[id] = 0
    }

    override fun visitArbeidsforhold(ansattFom: LocalDate, ansattTom: LocalDate?, deaktivert: Boolean) {
        arbeidsforhold.compute(innslag.last()) { _, oldValue -> oldValue!! + 1 }
    }
}