package no.nav.helse.serde.api

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import java.time.LocalDateTime
import java.util.*

fun hendelseReferanserForPerson(person: Person): MutableSet<UUID> {
    val hendelseVisitor = HendelseVisitor()
    person.accept(hendelseVisitor)
    return hendelseVisitor.hendelsereferanser
}

internal class HendelseVisitor : PersonVisitor {
    internal val hendelsereferanser = mutableSetOf<UUID>()

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID?,
        tidsstempel: LocalDateTime
    ) {
        if (id == null) return
        hendelsereferanser.add(id)
    }

    override fun visitInntekt(inntekt: Inntekthistorikk.Inntekt, id: UUID) {
        hendelsereferanser.add(id)
    }
}
