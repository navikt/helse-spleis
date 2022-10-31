package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Refusjonsopplysning
import no.nav.helse.person.RefusjonsopplysningerVisitor
import no.nav.helse.økonomi.Inntekt

internal val Refusjonsopplysning.Refusjonsopplysninger.inspektør get() = RefusjonsopplysningerInspektør(this)

internal class RefusjonsopplysningerInspektør(refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger): RefusjonsopplysningerVisitor {
    private val visitedRefusjonsopplysninger = mutableListOf<Refusjonsopplysning>()
    val refusjonsopplysninger get() = visitedRefusjonsopplysninger.toList()
    init {
        refusjonsopplysninger.accept(this)
    }
    override fun visitRefusjonsopplysning(meldingsreferanseId: UUID, fom: LocalDate, tom: LocalDate?, beløp: Inntekt) {
        visitedRefusjonsopplysninger.add(Refusjonsopplysning(meldingsreferanseId, fom, tom, beløp))
    }
}